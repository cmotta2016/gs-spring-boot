timestamps{
    node('mavenoci'){
        stage('Checkout'){
           //checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cmotta2016/gs-spring-boot.git']]])
            checkout scm
        }
        stage('Compile'){
            sh 'mvn -gs /home/jenkins/.m2/settings.xml clean install'
        }
        stage('Code Quality'){
            withSonarQubeEnv('SonarQube') { 
                sh 'mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar'
            }
        }
        stage('Quality gate'){
            timeout(activity: true, time: 15, unit: 'SECONDS') {
                sleep(10)
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }
            }
        }
        stage('Build S2I'){
            sh 's2i build . fabric8/s2i-java k8s-images/maven --loglevel 1 --network host'
        }
        stage('Push Image'){
            withCredentials([usernamePassword(credentialsId: 'nexus_oci', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
            sh '''
            docker login -u "$DOCKER_USERNAME" -p "$DOCKER_PASSWORD"
            docker push k8s-images/maven
            docker rmi -f k8s-images/maven
            '''
            }
        }
        stage('Deploy'){
            sh 'kubectl apply -f maven-template.yml -n maven'
        }        
    }
}
