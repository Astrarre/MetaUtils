REM       runCommand("git add .", workingDirectory = metaUtilsDir)
REM        runCommand("git commit -m \"$commitMessage\"", workingDirectory = metaUtilsDir)
REM        runCommand("git push", workingDirectory = metaUtilsDir)
REM        runCommand("git submodule update --remote")
REM        runCommand("git add .")
REM       runCommand("git commit -m \"$commitMessage\"")
REM       runCommand("git push")

cd MetaUtils
git add .
git commit -m "%1"
git push
