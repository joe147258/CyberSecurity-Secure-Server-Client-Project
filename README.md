# Secure Server / Client java program   
This is an application that was made as coursework for my Degree in the Foundations of Cybersecurity module. 
In this application clients can communicate through a server.
The messages stored on the server are completely encrypted with RSA and AES.
Signatures are used for verification.
To send messages to a user you need their private key (that is stored in the directory).
You must run the server and can connect as a client.   
Arguments:   
java Server *port*   
java Client *host* *port* *userid*    
If you have any questions please contact me at: jldphillips@gmail.com

