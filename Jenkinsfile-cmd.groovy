// globals var 4 openShift
def tagImage = "0.0.0"
def artifactVersion = ""

pipeline {

    agent any
    environment {

        //Project
        APP_JAR_PATH = 'demo-0.0.1-SNAPSHOT.jar'
        MAVEN_PATH = '/opt/jenkins_tools/apache-maven-3.6.0/bin'
        APP_PORT_TO_REPLACE = "8080"
        APP_PORT = "8080"

        //Deploy
        PRODUCTION_DEPLOY_NODE = 'QaNode'
        QA_DEPLOY_NODE = 'QaNode'
        DEV_DEPLOY_NODE = 'DevNode'

        //Openshift
        OPENSHIFT_CREDENTIAL_NAME = 'LUGTER23-TOKEN'
        OPENSHIFRT_IMAGE_NAME = 'openshift/ubi8-openjdk-11:1.3'
        OPENSHIFT_CLUSTER_DEV_QA_URL = 'https://api.5cd1a00d-8e69-4031-ae4f-2f844396412a.openshift.com'
        OPENSHIFT_NAMESPACE_DEV = 'lugter23-dev'
        OPENSHIFT_APP_NAME = 'demo-git'
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
                            sh "oc login --token=sha256~ru2Ir995j1Er-Iag8sVwviFvzysOM6p-fcAWAt3Qi9w --server=https://api.sandbox.x8i5.p1.openshiftapps.com:6443 --insecure-skip-tls-verify=true"
                        }
                        echo "${sh "oc whoami"}"
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
