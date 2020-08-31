timestamps{
    node('maven'){
        stage('Checkout'){
           //checkout([$class: 'GitSCM', branches: [[name: '*/openshift']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cmotta2016/gs-spring-boot.git']]])
           checkout scm
        }//stage
        stage('Compile'){
            sh 'mvn clean install'
        }//stage
        stage('Code Quality'){
            withSonarQubeEnv('SonarQube') { 
                sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar'
            }//withSonarQubeEnv
        }//stage
        /*stage('Quality Gate'){
            timeout(activity: true, time: 30, unit: 'SECONDS') {
                sleep(30)
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
                        openshift.newBuild("--name=${NAME}", "--image-stream=${IMAGE_BUILDER}", "--binary", "-l app=${LABEL}")
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
        		stage('Deploy QA') {
                    echo "Creating Secret Environments"
                    sh 'cat environments common > .env_qa'
                    def envSecret = openshift.apply(openshift.raw("create secret  generic environments --from-env-file=.env_qa --dry-run --output=yaml").actions[0].out)
                    envSecret.describe()
		            echo "Applying Template QA"
                    openshift.apply(openshift.process(readFile(file:"${TEMPLATE}-qa.yml"), "--param-file=template_environments"))
		            echo "Starting Deployment QA"
                    openshift.selector("dc", "${NAME}").rollout().latest()
                    def dc = openshift.selector("dc", "${NAME}")
                    dc.rollout().status()
                }//stage
                stage('Promote to HML'){
                    //routeHost = sh(script: "kubectl get ingress nodejs -n nodejs-qa -o jsonpath='{ .spec.rules[0].host }'", returnStdout: true).trim()
                    routeHost = openshift.raw("get route ${NAME} -o jsonpath='{ .spec.host }' --loglevel=4").out.trim()
                    input message: "Promote to HML. Test deployment: http://${routeHost}. Approve?", id: "approval"
                }
            }//withProject
            openshift.withProject("${PROJECT}-hml") {
                stage('Deploy HML') {
                    echo "Creating Secret Environments"
                    sh 'cat environments common > .env_hml'
                    def envSecret = openshift.apply(openshift.raw("create secret  generic environments --from-env-file=.env_hml --dry-run --output=yaml").actions[0].out)
                    envSecret.describe()
		    echo "Applying Template HML"
                    openshift.apply(openshift.process(readFile(file:"${TEMPLATE}-hml.yml"), "--param-file=template_environments"))
		    echo "Starting Deployment HML"
                    openshift.selector("dc", "${NAME}").rollout().latest()
                    def dc = openshift.selector("dc", "${NAME}")
                    dc.rollout().status()
                }//stage
            }//withProject
        }//withCluster
    }//node
}//timestamps
