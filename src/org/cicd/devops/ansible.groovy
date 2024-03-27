package org.cicd.devops

import org.cicd.utils.utils


/**
 * 发布代码
 * @param deployHosts
 * @return
 */
def deploy(Map params) {
    def utils = new utils()
    utils.printMessage("部署代码", "green")
    //获取ansible脚本
    initAllAnsibleCode(params)
    withCredentials([string(credentialsId: 'apollo_token', variable: 'apolloToken'), usernamePassword(credentialsId: "${params.get("env_info")}_nexus", passwordVariable: 'password', usernameVariable: 'username')]) {
        println("开始部署")
        params.put('username', "${username}")
        params.put('password', "${password}")
        params.put('token_info', "${apolloToken}")
        executePlaybookWithPath(params)
    }

    utils.printMessage("部署成功", "green")
}


/**
 * 初始化所有ansible脚本
 * @param params
 * @return
 */
def initAllAnsibleCode(Map params){
    //获取ansible执行脚本
    initAnsibleCode(params,"${params.get("playbook_main_repo")}", "${params.get("common_branch")}")
    //获取ansible配置脚本
    initAnsibleCode(params, "ops-${params.get("app_code")}", "${params.get("project_conf_branch")}")
}

/**
 * 初始化ansible代码
 * @param params
 * @param invokeName
 * @param branchName
 * @return
 */
def initAnsibleCode(Map params, String invokeName, String branchName) {
    def utils = new utils()
    def gitServer = new gitServer()
    utils.initTmpDir("${params.get("ansible_src")}/${invokeName}")
    dir("${params.get("ansible_src")}/${invokeName}") {
        gitServer.checkOut("${params.get("ansible_source_url")}/${invokeName}.git", branchName)
    }
}

/**
 * 组装执行路径
 * @param params
 * @return
 */
def executePlaybookWithPath(Map params){
    String playbookPath = "${params.get("ansible_src")}/ops-common/main.yml"
    String inventoryPath = "${params.get("ansible_src")}/ops-${params.get("app_code")}/inventory/inventory-${params.get("env_info")}.yml"
    executePlaybook(params, playbookPath, inventoryPath)
}



/**
 * 执行ansible剧本
 * @param params
 * @param playbookPath
 * @param inventoryPath
 * @return
 */
def executePlaybook(Map params, String playbookPath, String inventoryPath) {
    ansiColor('xterm') {
        println("执行ansible脚本")
        ansiblePlaybook(
                colorized: true,
                credentialsId: "ansible_user",
                installation: 'Ansible',
                playbook: playbookPath,
                inventory: inventoryPath,
                extraVars: params
        )
    }
}

