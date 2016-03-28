def projectName = "${PROJECT_NAME}"
def gitUrl = "${GIT_URL}"
def leaderEmail = "${LEADER_EMAIL}"
def stashCredentialId = "${STASH_CREDENTIAL_ID}"
def stashBaseUrl = 'https://cipcssmc.carrefour.com/stash'


folder("${projectName}")

job("${projectName}/bitbucket_trigger_pr") {
  parameters {
      stringParam('PULL_REQUEST_FROM_HASH', '', '')
      stringParam('PULL_REQUEST_AUTHOR_EMAIL', '', '')
  }
  logRotator {
      numToKeep(5)
      artifactNumToKeep(1)
  }
  steps {
    downstreamParameterized {
        trigger("${projectName}/deploy_int0") {
            block {
                buildStepFailure('FAILURE')
                failure('FAILURE')
                unstable('UNSTABLE')
            }
            parameters {
                predefinedProps(
                  [
                    COMMIT_HASH: '${PULL_REQUEST_FROM_HASH}',
                    AUTHOR_EMAIL: '${PULL_REQUEST_AUTHOR_EMAIL}'
                  ]
                )
            }
        }
    }
    publishers {
        mailer("${leaderEmail}", false, true)
        stashNotifier {
          stashServerBaseUrl(stashBaseUrl)
          credentialsId("${stashCredentialId}")
          commitSha1('${PULL_REQUEST_FROM_HASH}')
          ignoreUnverifiedSSLPeer()
        }
    }
  }
}

job("${projectName}/bitbucket_trigger_pr_merged") {
    scm {
        git(gitUrl)
    }
    triggers {
        scm('*/15 * * * *')
    }
    steps {
        shell('echo bitbucket_trigger_pr_merged')
    }
}

job("${projectName}/analyze_sonar") {
    scm {
        git(gitUrl)
    }
    triggers {
        cron('15 13 * * *')
    }
    steps {
        shell('echo analyze_sonar')
    }
}

job("${projectName}/build_env") {
    scm {
        git(gitUrl)
    }
    triggers {
        cron('15 13 * * *')
    }
    steps {
        shell('echo build_env')
    }
}

job("${projectName}/deploy") {
    scm {
        git(gitUrl)
    }
    triggers {
        cron('15 13 * * *')
    }
    steps {
        shell('echo deploy')
    }
}

job("${projectName}/package_from_branch") {
    scm {
        git(gitUrl)
    }
    triggers {
        cron('15 13 * * *')
    }
    steps {
        shell('echo package_from_branch')
    }
}

job("${projectName}/package_from_commit_hash") {
    scm {
        git(gitUrl)
    }
    triggers {
        cron('15 13 * * *')
    }
    steps {
        shell('echo package_from_commit_hash')
    }
}
