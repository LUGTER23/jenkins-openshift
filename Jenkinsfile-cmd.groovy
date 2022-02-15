// globals var 4 openShift
def tagImage = "0.0.0"
def artifactVersion = ""

pipeline {

    agent any
    environment {

        //Project
        APP_JAR_PATH = 'demo-0.0.1-SNAPSHOT.jar'
        MAVEN_PATH = 'C:/apache-maven-3.6.3/bin'
        APP_PORT_TO_REPLACE = "8080"
        APP_PORT = "8080"
        SECRET = ""

        //Deploy
        PRODUCTION_DEPLOY_NODE = 'QaNode'
        QA_DEPLOY_NODE = 'QaNode'
        DEV_DEPLOY_NODE = 'DevNode'

        //Openshift
        OPENSHIFT_CREDENTIAL_NAME = 'LUGTER23-TOKEN'
        OPENSHIFT_IMAGE_NAME = 'openshift/ubi8-openjdk-11:1.3'
        OPENSHIFT_CLUSTER_DEV_URL = 'https://api.sandbox.x8i5.p1.openshiftapps.com:6443'
        OPENSHIFT_NAMESPACE_DEV = 'lugter23-dev'
        OPENSHIFT_APP_NAME = 'demo'
        OPENSHIFT_MS_PORT = '8080'
        OPENSHIFT_REGISTRY = 'image-registry.openshift-image-registry.svc:5000'

    }

    stages {

        stage('Set Openshift') {
            steps {
                script {
                    def ocDir = tool "oc-client"
                    echo "${ocDir}"

                    withEnv(["PATH=${ocDir}:$PATH"]) {
                        withCredentials([string(credentialsId: OPENSHIFT_CREDENTIAL_NAME, variable: 'TOKEN_OCP')]) {
                            sh "oc login --token=${TOKEN_OCP} --server=${OPENSHIFT_CLUSTER_DEV_URL} --insecure-skip-tls-verify=true"
                        }
                        // comands for dev
                        sh "oc project ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc get bc/${OPENSHIFT_APP_NAME} --no-headers -o custom-columns=:metadata.name --ignore-not-found=true -n ${OPENSHIFT_NAMESPACE_DEV}"

                        def build_config = sh(script: "oc get bc/${OPENSHIFT_APP_NAME} --no-headers -o custom-columns=:metadata.name --ignore-not-found=true -n ${OPENSHIFT_NAMESPACE_DEV}"
                                , returnStdout: true).trim();


                        sh "oc delete bc/${OPENSHIFT_APP_NAME} --ignore-not-found=true -n ${OPENSHIFT_NAMESPACE_DEV}"
                        sh "oc delete is/${OPENSHIFT_APP_NAME} --ignore-not-found=true -n ${OPENSHIFT_NAMESPACE_DEV}"
                        sh "oc delete dc/${OPENSHIFT_APP_NAME} --ignore-not-found=true -n ${OPENSHIFT_NAMESPACE_DEV}"
                        sh "oc delete dc/${OPENSHIFT_APP_NAME} --ignore-not-found=true -n ${OPENSHIFT_NAMESPACE_DEV}"
                        sh "oc delete all -l app=${OPENSHIFT_APP_NAME} -n ${OPENSHIFT_NAMESPACE_DEV}"
                        if (build_config == OPENSHIFT_APP_NAME) {
                            echo "${build_config}"
                            sh "oc delete all -l app=${OPENSHIFT_APP_NAME} -n ${OPENSHIFT_NAMESPACE_DEV}"
                        }
                        def secret=sh(script: "oc get secret test -o jsonpath='{.data.test1}'",returnStdout: true).trim()
                        SECRET = secret
                        echo "${SECRET}"
                        sh "oc new-build --binary=true --strategy=source --name=${OPENSHIFT_APP_NAME} --image-stream=${OPENSHIFT_IMAGE_NAME} -e test1=${secret}"
                        sh "oc new-app ${OPENSHIFT_NAMESPACE_DEV}/${OPENSHIFT_APP_NAME}:latest --name=${OPENSHIFT_APP_NAME} --allow-missing-imagestream-tags=true -n ${OPENSHIFT_NAMESPACE_DEV} --as-deployment-config"
                        sh "oc set resources dc ${OPENSHIFT_APP_NAME} --limits=memory=400Mi,cpu=200m --requests=memory=300Mi,cpu=100m -n ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc set triggers dc/${OPENSHIFT_APP_NAME} --remove-all -n ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc delete configmap ${OPENSHIFT_APP_NAME}-config --ignore-not-found=true -n ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc create configmap ${OPENSHIFT_APP_NAME}-config -n ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc set volume dc/${OPENSHIFT_APP_NAME} --add --name=${OPENSHIFT_APP_NAME}-config --mount-path=/deployments/config/application.properties --sub-path=application.properties --configmap-name=${OPENSHIFT_APP_NAME}-config -n ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc delete svc/${OPENSHIFT_APP_NAME} --ignore-not-found=true -n ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc expose dc ${OPENSHIFT_APP_NAME} --port ${OPENSHIFT_MS_PORT} -n ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc create route edge --service=${OPENSHIFT_APP_NAME} -n ${OPENSHIFT_NAMESPACE_DEV}"
                    }
                }
            }
        }

        stage('Prepare') {
            steps {
                echo 'started maven clean'
                labelledShell(label: "Clean",
                        script: '${MAVEN_PATH}/mvn -U clean')
            }
        }

        stage('Build') {
            steps {
                echo 'started maven build'
                labelledShell(label: "Build",
                        script: 'export JAVA_HOME="C:/Program Files/Java/jdk-15" && java -version && ${MAVEN_PATH}/mvn clean -DskipTests compile')
            }
        }

        stage('Unit Test') {
            steps {
                echo "${SECRET}"
                labelledShell(label: "Execute Unite Test",
                        script: 'export JAVA_HOME="C:/Program Files/Java/jdk-15" && java -version && ${MAVEN_PATH}/mvn test')
            }
        }

        stage('Package') {
            steps {
                echo 'started maven package'
                labelledShell(label: "Package",
                        script: 'export JAVA_HOME="C:/Program Files/Java/jdk-15" && java -version && ${MAVEN_PATH}/mvn -Dmaven.test.skip=true package')
            }
        }

        stage('Build Image') {
            steps {
                echo "Build image started..."
                script {
                    def ocDir = tool "oc-client"
                    echo "${ocDir}"

                    withEnv(["PATH=${ocDir}:$PATH"]) {

                        withCredentials([string(credentialsId: OPENSHIFT_CREDENTIAL_NAME, variable: 'TOKEN_OCP')]) {

                            sh "oc login --token=${TOKEN_OCP} ${OPENSHIFT_CLUSTER_DEV_URL} --insecure-skip-tls-verify=true"
                        }
                        //getting version from artifatory
                        echo "read pom"
                        def pom = readMavenPom file: 'pom.xml'
                        def version = pom.version
                        tagImage = pom.version + "-" + currentBuild.number

                        artifactName = pom.artifactId
                        artifactVersion = pom.version
                        def nameJar = artifactName + "-" + artifactVersion + ".jar"

                        echo "${tagImage}"
                        echo "${nameJar}"

                        sh "oc start-build ${OPENSHIFT_APP_NAME} --from-file=./target/${nameJar} --wait=true -n ${OPENSHIFT_NAMESPACE_DEV}"
                        sh "oc tag ${OPENSHIFT_APP_NAME}:latest ${OPENSHIFT_APP_NAME}:${tagImage} -n ${OPENSHIFT_NAMESPACE_DEV}"

                    }
                }
                echo "End build image."

            }

        }

        stage('Deploy DEV') {
            steps {
                echo " deploying containter image to dev started..."

                script {
                    def ocDir = tool "oc-client"
                    echo "${ocDir}"

                    withEnv(["PATH=${ocDir}:$PATH"]) {
                        withCredentials([string(credentialsId: OPENSHIFT_CREDENTIAL_NAME, variable: 'TOKEN_OCP')]) {

                            sh "oc login --token=${TOKEN_OCP} ${OPENSHIFT_CLUSTER_DEV_URL} --insecure-skip-tls-verify=true"

                        }

                        sh "oc delete cm ${OPENSHIFT_APP_NAME}-config --ignore-not-found=true -n ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc create cm ${OPENSHIFT_APP_NAME}-config --from-file=./src/main/resources/application.properties -n ${OPENSHIFT_NAMESPACE_DEV}"

                        sh "oc set image dc/${OPENSHIFT_APP_NAME} ${OPENSHIFT_APP_NAME}=${OPENSHIFT_NAMESPACE_DEV}/${OPENSHIFT_APP_NAME}:${tagImage}" +
                                " --source=imagestreamtag -n ${OPENSHIFT_NAMESPACE_DEV}"
                        sh "oc rollout latest dc/${OPENSHIFT_APP_NAME} -n ${OPENSHIFT_NAMESPACE_DEV}"

                        def dc_version = sh(script: "oc get dc/${OPENSHIFT_APP_NAME} -o=yaml -n ${OPENSHIFT_NAMESPACE_DEV} | grep 'latestVersion'| cut -d':' -f 2",
                                returnStdout: true).trim();
                        echo "Current Version DeploymentConfig ${dc_version}"



                    }
                }
            }
        }
    }
}

/**
 * Metodo encargado de leer una archico de propiedades y reemplazar los valores en en achivo destino.
 *
 * En el archivo destino se buscan comodides de la estructura ${var}*
 * @param valuesPropertiesFile
 * @param templateFile
 * @param destinationFile
 * @return
 */
def replaceValuesInFile(valuesPropertiesFile, templateFile, destinationFile) {
    def props = readProperties file: valuesPropertiesFile

    def textTemplate = readFile templateFile
    echo "Contenido leido del template: " + textTemplate

    props.each { property ->
        echo property.key
        echo property.value
        textTemplate = textTemplate.replace('${' + property.key + '}', property.value)
    }

    echo "Contenido Reemplazado: " + textTemplate

    finalText = textTemplate
    writeFile(file: destinationFile, text: finalText, encoding: "UTF-8")
}
