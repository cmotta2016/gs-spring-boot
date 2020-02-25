timestamps{
    node('maven'){
        stage('Checkout'){
           //checkout([$class: 'GitSCM', branches: [[name: '*/openshift']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cmotta2016/gs-spring-boot.git']]])
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
                        openshift.newBuild("--name=${NAME}", "registry.redhat.io/openjdk/openjdk-8-rhel8", "--binary", "-l app=${LABEL}")
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
                    openshift.apply(openshift.process(readFile(file:'template-maven.yml'), "--param-file=jenkins.properties"))
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
                    openshift.apply(openshift.process(readFile(file:'template-maven.yml'), "--param-file=jenkins.properties"))
		    echo "Starting Deployment HML"
                    openshift.selector("dc", "${NAME}").rollout().latest()
                    def dc = openshift.selector("dc", "${NAME}")
                    dc.rollout().status()
                }//stage
            }//withProject
	    openshift.withProject("${PROJECT}-prd") {
                stage('Deploy PRD') {
                    echo "Creating Secret Environments"
                    sh 'cat environments common > .env_hml'
                    def envSecret = openshift.apply(openshift.raw("create secret  generic environments --from-env-file=.env_hml --dry-run --output=yaml").actions[0].out)
                    envSecret.describe()
		    echo "Applying Template HML"
                    openshift.apply(openshift.process(readFile(file:'template-maven.yml'), "--param-file=jenkins.properties"))
		    echo "Starting Deployment HML"
                    openshift.selector("dc", "${NAME}").rollout().latest()
                    def dc = openshift.selector("dc", "${NAME}")
                    dc.rollout().status()
                }//stage
            }//withProject
        }//withCluster
    }//node
}//timestamps
