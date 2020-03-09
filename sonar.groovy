def codeQuality(){
    def final scannerHome = tool name: 'SonarQube Scanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
           
    def version = sh (script: 'PACKAGE_VERSION=$(cat package.json | grep version | head -1 | awk -F: \'{ print $2 }\' | sed \'s/[\\",]//g\' | tr -d \'[[:space:]]\') && echo $PACKAGE_VERSION', returnStdout: true).trim()
    def git_branch = sh (script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
    git_branch = (git_branch == 'master' || git_branch == 'HEAD')?'': git_branch
    def git_repoURL = sh (script: 'git config --get remote.origin.url', returnStdout: true).trim()
   
    sonarProperties = readFile 'sonar-project.properties'
    sonarProperties = sonarProperties?.replaceAll('@version', version)?.replaceAll('@git_branch', git_branch)?.replaceAll('@git_repoURL', git_repoURL)?.replaceAll('@job_URL', "${JOB_URL}")
   
    // Fazer o print
    println sonarProperties
    println(scannerHome)
    writeFile encoding: 'UTF-8', file: 'sonar-project.properties', text: sonarProperties
    sh 'ls -las /'
    sh 'java -version'
    withSonarQubeEnv('SonarQube'){
        sh "${scannerHome}/bin/sonar-scanner"
    }
}
return this;
