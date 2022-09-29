pipeline {
    agent {
        label 'linux'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '40', artifactNumToKeepStr: '40'))
        timeout(time: 1, unit: 'HOURS')
    }

    parameters {
        booleanParam(name: 'skipTests', defaultValue: false, description: 'Skal tester kjøres.')
    }

    tools {
        maven 'maven'
        jdk 'openjdk11'
    }

    environment {
        GIT_SHA = sh(returnStdout: true, script: 'git rev-parse HEAD').substring(0, 7)
        WORKSPACE = pwd()
    }

    stages {

        stage('Initialize') {
            steps {
                print "Params: ${params}"
                script {
                    env.REPO_NAME = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
                    env.VERSION = readFile "${env.WORKSPACE}/version"
                    env.CURRENT_VERSION = env.VERSION.replace("SNAPSHOT", env.GIT_SHA)
                    env.user = buildUser()
                    env.GIT_BRANCH = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD')
                }
                echo(""" 
                    GIT_BRANCH = ${env.GIT_BRANCH}
                    GIT_SHA = ${env.GIT_SHA}
                    REPO_NAME = ${env.REPO_NAME}
                    user = ${env.user}
                    PATH = ${env.PATH}
                    M2_HOME = ${env.M2_HOME}
                    WORKSPACE = ${env.WORKSPACE}
                    CURRENT_VERSION = ${env.CURRENT_VERSION}
                    """)

                rtMavenDeployer (
                    id: "MAVEN_DEPLOYER",
                    serverId: "KS Artifactory",
                    releaseRepo: "ks-maven",
                    snapshotRepo: "maven-all"
                )
                rtMavenResolver (
                    id: "MAVEN_RESOLVER",
                    serverId: "KS Artifactory",
                    releaseRepo: "maven-all",
                    snapshotRepo: "maven-all"
                )
                rtBuildInfo(
                    captureEnv: true
                )
            }
        }

        stage('Build dhis2') {
            steps {
                configFileProvider([configFile(fileId: 'artifactory-settings.xml', variable: 'MAVEN_SETTINGS')]) {
                    rtMavenRun(
                        pom: 'dhis-2/pom.xml',
                        goals: "-T0.5C -U -B -s $MAVEN_SETTINGS clean install",
                        opts: "-DskipTests=${params.skipTests}",
                        resolverId: 'MAVEN_RESOLVER',
                        deployerId: "MAVEN_DEPLOYER",
                        tool: 'maven'
                    )
                    rtMavenRun(
                        pom: 'dhis-2/dhis-web/pom.xml',
                        goals: "-T0.5C -U -B -s $MAVEN_SETTINGS clean install",
                        opts: "-DskipTests=${params.skipTests}",
                        resolverId: 'MAVEN_RESOLVER',
                        deployerId: "MAVEN_DEPLOYER",
                        tool: 'maven'
                    )
                }
            }
            post {
                success {
                    recordIssues enabledForFailure: true, tools: [java(), mavenConsole()]
                    warnError(message: "Jacoco feilet") {
                        jacoco(execPattern: '**/*.exec')
                    }
                }
                always {
                    junit testResults:'**/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build and deploy') {
            when {
                branch 'main_ks'
            }
            steps {
                build job: 'KS/dhis2-setup/master', parameters: [booleanParam(name: 'isTriggeredFromDhis2Core', value: true), string(name: 'tag_dhis2_core', value: env.GIT_SHA), string(name: 'branch_dhis2_core', value: env.GIT_BRANCH)], wait: true, propagate: false
            }
       }
    }

    post {
        always {
            rtPublishBuildInfo(
                    serverId: "KS Artifactory"
            )
            archiveArtifacts artifacts: '**/build.log', fingerprint: true, allowEmptyArchive: true
            deleteDir()
        }
    }
}

def buildUser() {
    wrap([$class: 'BuildUser']) {
        return sh(script: 'echo "${BUILD_USER}"', returnStdout: true).trim()
    }
}
