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
        stage('Quality Gate'){
            timeout(activity: true, time: 15, unit: 'SECONDS') {
                sleep(10)
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }//if
            }//timeout
        }//stage
        openshift.withCluster() {
            openshift.withProject('maven-backend-qa') {
                stage('Create Build'){
                    echo "Criando build config da imagem final..."
                    if (!openshift.selector("bc", "maven-spring").exists()) {
                        openshift.newBuild("--name=maven-spring", "openshift/java", "--binary", "-l app=maven")
                        def build = openshift.selector("bc", "maven-spring").startBuild("--from-dir=target")
                        build.logs('-f')
                    }//if
                    else {
                        def build = openshift.selector("bc", "maven-spring").startBuild("--from-dir=target")
                        build.logs('-f')
                    }
                    }//stage
            }//withProject
            openshift.withProject('maven-backend-prd') {
                stage('Deploy') {
                    openshift.selector("dc", "maven-spring").rollout()
                    def dc = openshift.selector("dc", "maven-spring")
                    dc.rollout().status()
                }//stage
            }//withProject
        }//withCluster
    }//node
}//timestamps
