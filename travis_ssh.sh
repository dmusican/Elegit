sudo ssh-keygen -q -f /etc/ssh/ssh_host_key -N '' -t rsa1
sudo ssh-keygen -f /etc/ssh/ssh_host_rsa_key -N '' -t rsa
sudo ssh-keygen -f /etc/ssh/ssh_host_dsa_key -N '' -t dsa
sudo service ssh restart

ssh -T travis@localhost exit
echo $?