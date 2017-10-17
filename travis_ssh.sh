ssh-keygen -R localhost
sshpass -p '' ssh -o StrictHostKeyChecking=no -T localhost exit
echo $?
