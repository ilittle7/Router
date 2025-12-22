"""
CI build script

Environment: linux, python3

Dependencies:
1. git
2. bcecmd
"""

import os
import sys
import subprocess
import json
import requests

ERROR_GRADLE_PUBLISH = 1

BUILD_TYPE_PUBLISH = "publish"

sys_argv = dict(enumerate(sys.argv))
BUILD_TYPE = sys_argv.get(1, BUILD_TYPE_PUBLISH)
BUILD_NUMBER = int(os.environ.get('AGILE_GIT_NUMBER', 0)) + 100
os.environ["BUILD_NUMBER"] = str(BUILD_NUMBER)


def sh(command):
    """
    Run a shell command and return the standard output.
    :param command: shell command
    :return: the result standard output string of the command execution
    """
    return subprocess.run(command, shell=True, stdout=subprocess.PIPE).stdout.decode("utf-8")


COMMIT_MESSAGE = sh(
    """git log HEAD -1 --pretty=%B | head -1 | sed 's/"/\\\“/g'""")
COMMIT_REPO_NAME = os.environ.get('AGILE_MODULE_NAME')
COMMIT_AUTHOR = os.environ.get('AGILE_CHECKIN_AUTHOR')
COMMIT_BRANCH = os.environ.get('AGILE_COMPILE_BRANCH')


def chatbot(message_array):
    """
    Send infoflow notification
    :param message_array: a dictionary of infoflow bot text
    """
    json_str = json.dumps({
        'message': {
            'body': message_array
        }
    })
    resp = requests.post(
        # suika test
        # 'http://apiin.im.baidu.com/api/msg/groupmsgsend?access_token=d1149cf0a5e43c846f8393f5c565c5c62',
        # butter android
        'http://apiin.im.baidu.com/api/msg/groupmsgsend?access_token=d06bb90f3a4faeba19a7c7fdd3dcc1f75',
        headers={'Content-Type': 'application/json'},
        data=bytes(json_str, 'utf-8')
    )
    print('Chatbot response: ', resp.text)


#
# Step 1: Build
#
print("---- Step 1: Build ----")

os.system("rm -r ./output")
os.system("mkdir ./output")

# Run gradle build command
error_code = 0
if BUILD_TYPE == BUILD_TYPE_PUBLISH:
    os.system('./gradlew --stop')
    os.system('./gradlew clean')
    os.system('./gradlew :router-plugin:assemble :router-lib:assemble'
              ' :router-processor:assemble :router-annotation:assemble')
    error_code = os.system('./gradlew :router-plugin:publish :router-lib:publish'
              ' :router-processor:publish :router-annotation:publish') and ERROR_GRADLE_PUBLISH
else:
    print("Unknown build type")
    exit(1)

if error_code != 0:
    """
    Notify build failed.
    """
    chatbot(
        [
            {
                'type': 'TEXT',
                'content': 'Gradle Build Fail'
            },
            {
                'type': 'AT',
                'atall': True
            },
            {
                'type': 'TEXT',
                'content': '\n\nMessage: {}'.format(COMMIT_MESSAGE) +
                           '\nRepository: {}'.format(COMMIT_REPO_NAME) +
                           '\nAuthor: {}'.format(COMMIT_AUTHOR) +
                           '\nBranch: {}\n\n['.format(COMMIT_BRANCH)
            },
            {
                'type': 'LINK',
                'href': "https://console.cloud.baidu-int.com/devops/ipipe/workspaces/" \
                        "{0}/pipeline-builds/{1}/stage-builds/{2}/view".format(
                    os.environ.get('AGILE_WORKSPACE_ID'),
                    os.environ.get('AGILE_PIPELINE_BUILD_ID'),
                    os.environ.get('AGILE_STAGE_BUILD_ID')),
                'label': 'log'
            },
            {
                'type': 'TEXT',
                'content': ']'
            },
        ]
    )

    exit(error_code)

# Notify infoflow
chatbot_msg_title = "Agile Build Success "
chatbot_msg_body = """

Message: {0}
Repository: {1}
Author: {2}
Branch: {3}
Build Number: {4}""".format(
    COMMIT_MESSAGE, COMMIT_REPO_NAME, COMMIT_AUTHOR, COMMIT_BRANCH, BUILD_NUMBER)

rob_msg_array = [
    {
        'type': 'TEXT',
        'content': chatbot_msg_title
    },
    {
        'type': 'TEXT',
        'content': chatbot_msg_body
    }
]

chatbot(rob_msg_array)
