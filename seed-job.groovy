def projectName = "${PROJECT_NAME}"
def gitUrl = "${GIT_URL}"

folder("${projectName}")

job("${projectName}/bitbucket_trigger_pr") {
    scm {
        git(gitUrl)
    }
    triggers {
        scm('*/15 * * * *')
    }
    steps {
        shell('echo bitbucket_trigger_pr')
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
