ssh-keygen -R localhost
ssh -o StrictHostKeyChecking=no -T travis@localhost exit
echo $?