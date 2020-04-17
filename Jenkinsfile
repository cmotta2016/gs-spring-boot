timestamps{
    def tag="blue"
    def altTag="green"
    def routeHost="${tag}-${NAME}-${PROJECT}-prd.apps.openshift.oracle.msdigital.pro"
    node('maven'){
        stage('Checkout'){
           //checkout([$class: 'GitSCM', branches: [[name: '*/blue-green']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cmotta2016/gs-spring-boot.git']]])
            checkout scm
        }//stage
        stage('Compile'){
            sh 'mvn clean install'
        }//stage
        /*stage('Code Quality'){
            withSonarQubeEnv('SonarQube') { 
                sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar'
            }//withSonarQubeEnv
        }//stage
        stage('Quality Gate'){
            timeout(activity: true, time: 15, unit: 'SECONDS') {
                sleep(10)
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }//if
            }//timeout
        }//stage*/
        openshift.withCluster() {
            openshift.withProject("${PROJECT}-qa") {
                stage('Build Image'){
                    echo "Creating Image"
                    if (!openshift.selector("bc", "${NAME}").exists()) {
                        openshift.newBuild("--name=${NAME}", "openshift/java", "--binary", "-l app=${LABEL}")
                        def build = openshift.selector("bc", "${NAME}").startBuild("--from-dir=target")
                        build.logs('-f')
                    }//if
                    else {
                        def build = openshift.selector("bc", "${NAME}").startBuild("--from-dir=target")
                        build.logs('-f')
                    }//else
                    }//stage
                stage('Tagging Image'){
		    openshift.tag("${NAME}:latest", "${REPOSITORY}/${NAME}:latest")
                    //openshift.tag("${NAME}:latest", "${REPOSITORY}/${NAME}:${tag}")
                }//stage
		/*stage('Deploy QA') {
                    echo "Creating Secret Environments"
                    sh 'cat environments common > .env_qa'
                    def envSecret = openshift.apply(openshift.raw("create secret  generic environments --from-env-file=.env_qa --dry-run --output=yaml").actions[0].out)
                    envSecret.describe()
		    echo "Applying Template QA"
                    openshift.apply(openshift.process(readFile(file:'template-qa.yml'), "--param-file=environments-template"))
		    echo "Starting Deployment QA"
                    openshift.selector("dc", "${NAME}").rollout().latest()
                    def dc = openshift.selector("dc", "${NAME}")
                    dc.rollout().status()
                }//stage
            }//withProject
            openshift.withProject("${PROJECT}-hml") {
                stage('Deploy HML') {
                    echo "Creating Secret Environments"
                    sh 'cat environments common > .env_hml'
                    def envSecret = openshift.apply(openshift.raw("create secret  generic environments --from-env-file=.env_hml --dry-run --output=yaml").actions[0].out)
                    envSecret.describe()
		    echo "Applying Template HML"
                    openshift.apply(openshift.process(readFile(file:'template-hml-prd.yml'), "--param-file=environments-template"))
		    echo "Starting Deployment HML"
                    openshift.selector("dc", "${NAME}").rollout().latest()
                    def dc = openshift.selector("dc", "${NAME}")
                    dc.rollout().status()
                }//stage*/
            }//withProject
            openshift.withProject("${PROJECT}-prd") {
                stage("Initialize Blue-Green Routes") {
                    if (openshift.selector("route", "${tag}-${NAME}").exists()) {
                      def activeService = openshift.raw("get route ${NAME} -o jsonpath='{ .spec.to.name }' --loglevel=4").out.trim()
                      if (activeService == "${NAME}-blue") {
                        tag = "green"
                        altTag = "blue"
                      }//if
                      routeHost = openshift.raw("get route ${tag}-${NAME} -o jsonpath='{ .spec.host }' --loglevel=4").out.trim()
                    }//if
                }//stage
                stage('Re-tagging Image'){
                    openshift.tag("${REPOSITORY}/${NAME}:latest", "${REPOSITORY}/${NAME}:${tag}")
                }//stage
                stage('Deploy PRD') {
                    echo "Creating Secret Environments"
                    sh 'cat environments common > .env_prd'
                    def envSecret = openshift.apply(openshift.raw("create secret  generic environments --from-env-file=.env_prd --dry-run --output=yaml").actions[0].out)
                    envSecret.describe()
                    echo "Applying Template PRD"
		    openshift.apply(openshift.process(readFile(file:'template-blue-green.yml'), "--param-file=environments-template"))
		    /*if (openshift.selector("route", "${NAME}").exists()) {
                    	openshift.apply(openshift.process(readFile(file:'template-blue-green.yml'), "--param-file=environments-template"), "-l name!=principal")
		    } else {
		    	openshift.apply(openshift.process(readFile(file:'template-blue-green.yml'), "--param-file=environments-template"))
		    }*/
                    echo "Starting Deployment PRD"
                    openshift.selector("dc", "${NAME}-${tag}").rollout().latest()
                    def dc = openshift.selector("dc", "${NAME}-${tag}")
                    dc.rollout().status()
                }//stage
                stage("Test Deployment") {
                    input message: "Test deployment: http://${routeHost}. Approve?", id: "approval"
                }//stage
                stage("Go Live") {
                    openshift.raw("set route-backends ${NAME} ${NAME}-${tag}=100 ${NAME}-${altTag}=0 --loglevel=4").out
                }//stage
            }//withProject
        }//withCluster
    }//node
}//timestamp
