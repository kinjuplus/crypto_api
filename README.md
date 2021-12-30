# Prerequisite
Any computer with Java 11 or higher and Apache maven installed
# How to build and launch this App?
## In the command prompt
1.  change working directory to the project root
2.  run 'mvn clean package -Dmaven.test.skip'
3.  change working directory to the target folder under project root
4.  run 'java -jar crypto_api-0.0.1-SNAPSHOT.jar'
5.  open your browser with http://localhost:8082 ( if you want to switch to another port, please change server port property in the application.yml to whatever you want)


