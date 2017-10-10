#!/usr/bin/python

import sys
from twisted.web.static import File
from twisted.python import log
from twisted.web.server import Site
from twisted.internet import reactor
from twisted.protocols.basic import LineReceiver
from twisted.internet import protocol, reactor, endpoints
import json

from autobahn.twisted.websocket import WebSocketServerFactory, \
    WebSocketServerProtocol

from autobahn.twisted.resource import WebSocketResource


# Websocket protocol handler used by WebSocketFactory to
# support communication with browser.
class WebSocketProtocol(WebSocketServerProtocol):
    def onOpen(self):
        """
        Connection from client is opened. Fires after opening
        websockets handshake has been completed and we can send
        and receive messages.
        """
        self.factory.register(self)

    def connectionLost(self, reason):
        """
        Client lost connection, either disconnected or some error.
        """
        self.factory.unregister(self)

    def onMessage(self, payload, isBinary):
        """
        Message sent from client, communicate this message to its conversation partner,
        """
        if not isBinary:
            print "client message received"
            self.factory.communicate(payload)
        else:
            print "was binary"

    def send(self, payload):
        # obj = json.loads(payload.decode('utf8'))
        # print obj
        # self.sendMessage(json.dumps(obj).encode('utf8'))
        self.sendMessage(payload)



# Socket protocol handler used by LocalSocketFactory to
# support communication with Arro server sw.
#class LocalSocketProtocol(protocol.Protocol):
class LocalSocketProtocol(LineReceiver):
    delimiter = '\n'

    def __init__(self, factory):
        self.factory = factory
        self.peer = self

        self.factory.register(self)

    def connectionLost(self, reason):
        """
        Client lost connection, either disconnected or some error.
        Remove client from list of tracked connections.
        """
        self.factory.unregister(self)
    def lineReceived(self, payload):
        # problem here: need to recognize separate messages.
        print 'LocalSocketProtocol LocalSocketProtocol ' + payload
        # use factory method to send message to browser
        self.factory.communicate(payload)
        #self.transport.write(payload)

    def send(self, payload):
        print "server message received"
        self.transport.write(payload)

# browser clients
clients = []
# arro server (could theoretically be more than 1)
servers = []


class LocalSocketFactory(protocol.Factory):
    def buildProtocol(self, addr):
        return LocalSocketProtocol(self)

    def register(self, server):
        print "register "
        servers.append(server)

    def unregister(self, server):
        print "unregister"
        servers.remove(server)

    def communicate(self, payload):
        for s in clients:
            s.send(payload)


class WebSocketFactory(WebSocketServerFactory):
    def __init__(self, *args, **kwargs):
        super(WebSocketFactory, self).__init__(*args, **kwargs)

    def register(self, client):
        print "register " + client.peer
        clients.append(client)

    def unregister(self, client):
        print "unregister"
        clients.remove(client)

    def communicate(self, payload):
        print "send to servers "
        for s in servers:
            s.send(payload)



if __name__ == "__main__":
    # allow logging
    log.startLogging(sys.stdout)

    # static file server seving index.html as root
    root = File(".")

    # create factory for ws and connect our protocol, creates
    # instance of WebSocketProtocol for each connection
    factory = WebSocketFactory(u"ws://127.0.0.1:8080")
    factory.protocol = WebSocketProtocol

    # connect the resource (web page) to the factory
    resource = WebSocketResource(factory)
    # and assign resource to a root URL ("/ws1")
    root.putChild(u"ws", resource)
 
    # start 'normal' socket
    endpoints.serverFromString(reactor, "tcp:9000").listen(LocalSocketFactory())


    # start web socket
    site = Site(root)
    reactor.listenTCP(8080, site)
    reactor.run()


