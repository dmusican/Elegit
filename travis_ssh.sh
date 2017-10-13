ssh-keygen -R localhost
ssh -o StrictHostKeyChecking=no -T localhost exit
echo $?