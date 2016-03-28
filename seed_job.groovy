def projectName = "${PROJECT_NAME}"
def sources_gitUrl = "${SOURCES_GIT_URL}"
def ansible_gitUrl = "${ANSIBLE_GIT_URL}"
def ansible_gitBranch = "${ANSIBLE_GIT_BRANCH}"
def ansible_playbook = "${ANSIBLE_PLAYBOOK}"
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
          //stashServerBaseUrl(stashBaseUrl)
          //credentialsId("${stashCredentialId}")
          commitSha1('${PULL_REQUEST_FROM_HASH}')
          //ignoreUnverifiedSSLPeer()
        }
    }
  }
}

job("${projectName}/bitbucket_trigger_pr_merged") {
    scm {
        git(sources_gitUrl)
    }
    triggers {
        scm('*/15 * * * *')
    }
    steps {
        shell('echo bitbucket_trigger_pr_merged')
    }
}

job("${projectName}/build_env") {
  parameters {
      choiceParam('INT_ENV',
      [
        'int0/int0',
        'int1/int1',
        'int2/int2'
      ],
      'Selected the environment to build.'
      )
  }
  logRotator {
      numToKeep(5)
      artifactNumToKeep(1)
  }
  scm {
      git {
          remote {
              name('origin')
              url(ansible_gitUrl)
          }
          branch(ansible_gitBranch)
          extensions {
          }
      }
  }
  configure { project ->
      project / builders / 'org.jenkinsci.plugins.ansible.AnsiblePlaybookBuilder'(plugin: "ansible@0.4") {
          playbook(ansible_playbook)
          inventory(class: "org.jenkinsci.plugins.ansible.InventoryPath") {
            path 'environment/${INT_ENV}'
          }
          ansibleName 'Ansible'
          forks '5'
      }
  }
}

job("${projectName}/analyze_sonar") {
    scm {
        git(sources_gitUrl)
    }
    triggers {
        cron('15 13 * * *')
    }
    steps {
        shell('echo analyze_sonar')
    }
}

job("${projectName}/deploy") {
    scm {
        git(sources_gitUrl)
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
        git(sources_gitUrl)
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
        git(sources_gitUrl)
    }
    triggers {
        cron('15 13 * * *')
    }
    steps {
        shell('echo package_from_commit_hash')
    }
}
