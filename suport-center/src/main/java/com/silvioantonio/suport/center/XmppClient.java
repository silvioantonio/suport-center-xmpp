package com.silvioantonio.suport.center;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;

public class XmppClient extends AbstractXmppClient {
    private String toJaberId;

    public static void main(String[] args) {
        try {
            try(XmppClient client = new XmppClient()){
                client.start();
            }
        }catch(RuntimeException e){
            System.err.println("Erro ao iniciar aplicação: " + e.getMessage());
        }
    }

    @Override
    public void sendMessageLoop() {
        System.out.println("Aguarde um instante que um funcionário irá atendê-lo.");
        super.sendMessageLoop();
    }

    @Override
    public void newIncomingMessage(EntityBareJid fromJabberId, Message message, Chat chat) {
        super.newIncomingMessage(fromJabberId, message, chat);

        this.toJaberId = fromJabberId.toString();

        if(isChatting()) {
            return;
        }

        if(message.getBody().equalsIgnoreCase("Está aguardando?")) {
            sendMessage(chat, "Estou aguardando sim");
        }else if(message.getBody().equalsIgnoreCase("Em que posso ajudá-lo?")){
            //Define o ID do funcionário que enviou a mensagem e que vai atender o cliente.
            setDestinationUser(fromJabberId);
        }
    }
}