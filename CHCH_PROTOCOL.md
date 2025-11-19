# CHCH protocol
Il s'agit d'une application de chat entre plusieurs utilisateurs. Chaque utilisateur peut choisir dans quel canal il veut discuter parmis ceux existant sur le server

## Section 1 - Overview
CHCH protocol à pour but de pouvoir envoyer des messages parmis les canaux existant entre les différents utilisateurs qui se connecteront au serveur.

## Section 2 - Transport protocol
CHCH protocol est un protocole de transfert de message. Il utilise TCP (Transmission Control Protocol) pour garantir la fiabilité de la transmission des données et il utilisera le port 4269

Chaque message doit être encodé en UTF-8. Les messages sont traités comme des messages texte.

La connexion initiale doit être établie par le client.

Une fois la connexion établie, le client peut se connecter au serveur avec un nom d'utilisateur donné et en choisissant le canal parmis ceux existant.

Si ces conditions sont remplies, le serveur autorise le client à se connecter.

Sinon, le serveur refuse l'accès au client.

Le client peut alors envoyer un message texte au serveur.

## Section 3 - Messages
### Join the server
The client sends a join message to the server indicating the client's username and channel name.

Request

```
JOIN <username> <channel name>
```

username: the name of the client

channel name: the name of the channel


Response
- OK: the client has been granted access to the server
- ERROR \<code>: an error occurred during the join.


### List channel
The client sends a message to the server to request the list of channel.

Request

```
CHANLIST
```
Response
- CHANLIST \<channel1> \<channel2> \<channel3> ... : the list of channel.


### Send a message


## Section 4 - Examples