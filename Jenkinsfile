timestamps{
    def tag="blue"
    def altTag="green"
    def routeHost="${tag}-${NAME}-${PROJECT}-qa.apps.openshift.oracle.msdigital.pro"
    node('maven'){
        stage('Checkout'){
           //checkout([$class: 'GitSCM', branches: [[name: '*/blue-green']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cmotta2016/gs-spring-boot.git']]])
            checkout scm
        }//stage
        stage('Compile'){
            sh 'mvn clean install'
        }//stage
        openshift.withCluster() {
            openshift.withProject("${PROJECT}-qa") {
                stage("Initializing Routes") {
                    if (openshift.selector("route", "${NAME}").exists()) {
                      def activeService = openshift.raw("get route ${NAME} -o jsonpath='{ .spec.to.name }' --loglevel=4").out.trim()
                      if (activeService == "${NAME}-blue") {
                        tag = "green"
                        altTag = "blue"
                      }//if
                      routeHost = openshift.raw("get route ${tag}-${NAME} -o jsonpath='{ .spec.host }' --loglevel=4").out.trim()
                    }//if
                }//stage
                stage('Build'){
                    echo "Criando build config da imagem final..."
                    if (!openshift.selector("bc", "${NAME}").exists()) {
                        openshift.newBuild("--name=${NAME}", "--image-stream=openshift/openjdk-8-rhel8", "--binary", "-l app=${LABEL}")
                        def build = openshift.selector("bc", "${NAME}").startBuild("--from-dir=target")
                        build.logs('-f')
                        }//if
                    else {
                        def build = openshift.selector("bc", "${NAME}").startBuild("--from-dir=target")
                        build.logs('-f')
                        }//else
                }//stage
                stage('Deploy QA') {
                    echo "Creating Secret Environments"
                    sh 'cat environments common > .env_qa'
                    def envSecret = openshift.apply(openshift.raw("create secret  generic environments --from-env-file=.env_qa --dry-run --output=yaml").actions[0].out)
                    envSecret.describe()
                    echo "Applying Template QA"
                    openshift.apply(openshift.process(readFile(file:'template-blue-green.yml'), "--param-file=jenkins.properties"))
                }//stage
                stage('Tagging Image'){
                    openshift.tag("${NAME}:latest", "${NAME}:${tag}")
                }//stage
                stage("Starting Deploy") {
                    echo "Starting Deploy QA"
                    openshift.selector("dc", "${NAME}-${tag}").rollout().latest()
                    def dc = openshift.selector("dc", "${NAME}-${tag}")
                    dc.rollout().status()
                }//stage
                stage("Test") {
                    input message: "Test deployment: http://${routeHost}. Approve?", id: "approval"
                }//stage
                stage("Go Live") {
                    openshift.raw("set -n ${PROJECT}-qa route-backends ${NAME} ${NAME}-${tag}=100 ${NAME}-${altTag}=0 --loglevel=4").out
                }//stage
            }//withProject
        }//withCluster
    }//node
}//timestamps
