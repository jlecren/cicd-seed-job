def projectName = "${PROJECT_NAME}"
def sources_gitUrl = "${SOURCES_GIT_URL}"
def archivePackage = "${ARCHIVE_PACKAGE}"
def ansible_gitUrl = "${ANSIBLE_GIT_URL}"
def ansible_gitBranch = "${ANSIBLE_GIT_BRANCH}"
def ansible_buildPlaybook = "${ANSIBLE_BUILD_PLAYBOOK}"
def ansible_deployPlaybook = "${ANSIBLE_DEPLOY_PLAYBOOK}"
def ansible_testPlaybook = "${ANSIBLE_TEST_PLAYBOOK}"
def leaderEmail = "${LEADER_EMAIL}"
def releaseMgrEmail = "${RELEASE_MGR_EMAIL}"
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
      project / builders / 'org.jenkinsci.plugins.ansible.AnsiblePlaybookBuilder'(plugin: "ansible@0.3.1") {
          playbook(ansible_buildPlaybook)
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

for(i in 0..2) {
  def inventoryPath = "environment/int${i}/int${i}"
  job("${projectName}/deploy_int${i}") {
    parameters {
        stringParam('COMMIT_HASH', '', '')
        stringParam('AUTHOR_EMAIL', '', '')
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
    steps {
      downstreamParameterized {
          trigger("${projectName}/package_from_commit_hash") {
              block {
                  buildStepFailure('FAILURE')
                  failure('FAILURE')
                  unstable('UNSTABLE')
              }
              parameters {
                currentBuild()
              }
          }
      }
    }
    configure { project ->
        project / builders << 'org.jenkinsci.plugins.ansible.AnsiblePlaybookBuilder'(plugin: "ansible@0.3.1") {
            playbook(ansible_deployPlaybook)
            inventory(class: "org.jenkinsci.plugins.ansible.InventoryPath") {
              path(inventoryPath)
            }
            ansibleName 'Ansible'
            forks '5'
            additionalParameters '-e "archive_name=archive_${COMMIT_HASH}.tar.bz2"'
        }
    }
    configure { project ->
        project / builders << 'org.jenkinsci.plugins.ansible.AnsiblePlaybookBuilder'(plugin: "ansible@0.3.1") {
            playbook(ansible_testPlaybook)
            inventory(class: "org.jenkinsci.plugins.ansible.InventoryPath") {
              path(inventoryPath)
            }
            ansibleName 'Ansible'
            forks '5'
        }
    }
    publishers {
        mailer(releaseMgrEmail, false, false)
    }
  }
}

job("${projectName}/package_from_branch") {
  parameters {
      stringParam('BRANCH', 'develop', '')
      stringParam('AUTHOR_EMAIL', '', '')
      choiceParam('DEPLOY_INT',
      [
        "${projectName}/deploy_int1",
        "${projectName}/deploy_int2"
      ],
      'Select the environment to deploy to.'
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
              url(sources_gitUrl)
          }
          branch('*/${BRANCH}')
          extensions {
            cleanBeforeCheckout()
            pruneBranches()
            relativeTargetDirectory('sources')
          }
      }
  }
  steps {
      shell('''cd sources
echo COMMIT_HASH=`git rev-parse --verify HEAD` > params.txt''')
      downstreamParameterized {
          trigger("${projectName}/package_from_commit_hash") {
              block {
                  buildStepFailure('FAILURE')
                  failure('FAILURE')
                  unstable('UNSTABLE')
              }
              parameters {
                propertiesFile('sources/params.txt', true)
              }
          }
      }
      downstreamParameterized {
          trigger('${DEPLOY_INT}') {
              block {
                  buildStepFailure('FAILURE')
                  failure('FAILURE')
                  unstable('UNSTABLE')
              }
              parameters {
                propertiesFile('sources/params.txt', true)
              }
          }
      }
  }
}

job("${projectName}/package_from_commit_hash") {
  parameters {
      stringParam('COMMIT_HASH', '', '')
      stringParam('AUTHOR_EMAIL', '', '')
  }
  logRotator {
      numToKeep(5)
      artifactNumToKeep(1)
  }
  scm {
      git {
          remote {
              name('origin')
              url(sources_gitUrl)
          }
          branch('${COMMIT_HASH}')
          extensions {
            cleanBeforeCheckout()
            pruneBranches()
            relativeTargetDirectory('sources')
          }
      }
  }
  steps {
      managedScript('Build PHP Archive') {
        arguments('snapshots', "${archivePackage}/archive_\${COMMIT_HASH}.tar.bz2", '${COMMIT_HASH}')
      }
  }
  publishers {
      archiveArtifacts('archive.tar.bz2')
  }
}
