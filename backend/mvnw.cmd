@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup script for Windows, version 3.2.0
@REM
@REM Optional ENV vars
@REM   JAVA_HOME - Java installation directory
@REM   MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM   MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@IF "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

@setlocal

@set ERROR_CODE=0

@REM To isolate internal variables from possible setmqenv environment variable,
@REM we use a prefix "MW_".
@set "MW_COMMAND=%~0"

@REM ==== START VALIDATION ====
@if not "%JAVA_HOME%" == "" goto OkJHome

@set "JAVA_EXE=java.exe"
@%JAVA_EXE% -version >NUL 2>&1
@if "%ERRORLEVEL%" == "0" goto OkJava

@echo.
@echo ERROR: JAVA_HOME not found in your environment.
@echo Please set the JAVA_HOME variable in your environment to match the
@echo location of your Java installation.
@echo.
@goto error

:OkJHome
@set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

:OkJava

@REM find the project root
@set "MW_PROJECT_ROOT=%~dp0"
:findRoot
@if exist "%MW_PROJECT_ROOT%\.mvn" goto foundRoot
@set "MW_PROJECT_ROOT=%MW_PROJECT_ROOT%..\"
@if "%MW_PROJECT_ROOT%" == "..\" goto error
@goto findRoot

:foundRoot
@set "MW_JAR=%MW_PROJECT_ROOT%\.mvn\wrapper\maven-wrapper.jar"
@set "MW_PROPERTIES=%MW_PROJECT_ROOT%\.mvn\wrapper\maven-wrapper.properties"

@REM check if we have to download the wrapper jar
@if exist "%MW_JAR%" goto run

@REM get the wrapper url from the properties file
@for /f "tokens=2 delims==" %%i in ('findstr /l /c:"wrapperUrl=" "%MW_PROPERTIES%"') do @set "MW_WRAPPER_URL=%%i"

@echo Downloading Maven Wrapper from %MW_WRAPPER_URL%
@powershell -Command "&{"^
  "$webclient = New-Object System.Net.WebClient;"^
  "if ('%MW_WRAPPER_URL%' -ne '') {"^
  "  $webclient.DownloadFile('%MW_WRAPPER_URL%', '%MW_JAR%');"^
  "} else {"^
  "  Write-Error 'wrapperUrl not found in %MW_PROPERTIES%';"^
  "}"^
"}"

:run
@set "MAVEN_OPTS=%MAVEN_OPTS% -Dmaven.multiModuleProjectDirectory=%MW_PROJECT_ROOT%"
@"%JAVA_EXE%" %MAVEN_OPTS% -classpath "%MW_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
@if ERRORLEVEL 1 goto error
@goto end

:error
@set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

@if not "%MAVEN_BATCH_PAUSE%" == "on" goto mainEnd
@pause

:mainEnd
@if %ERROR_CODE% neq 0 @exit /B %ERROR_CODE%
@exit /B 0
