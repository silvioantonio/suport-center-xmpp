package com.silvioantonio.suport.center;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.EntityBareJid;

public class SuportCenter extends AbstractXmppClient {
    
    public static void main(String[] args) {
        try {
            try(SuportCenter client = new SuportCenter()){
                client.start();
            }
        }catch(RuntimeException e){
            System.err.println("Erro ao iniciar aplicação: " + e.getMessage());
        }
    }

    @Override
    public void sendMessageLoop() {
        System.out.println("Aguarde conexão de cliente para iniciar atendimento.");
        super.sendMessageLoop();
    }

    @Override
    protected void afterConnected() {
        //Carregar lista de contatos.
        if(!getRoster().isLoaded()){
            try {
                getRoster().reloadAndWait();
            } catch (SmackException.NotLoggedInException | InterruptedException | SmackException.NotConnectedException e) {
                throw new RuntimeException("Não foi possível obter a lista de clientes: " + e.getMessage());
            }
        }

        System.out.println("Total de clientes nos contatos: " + getRoster().getEntries().size());
        getRoster().getEntries().forEach( contact -> {
            try {
                getRoster().sendSubscriptionRequest(contact.getJid().asBareJid());
                System.out.println("\t"+contact.getJid());
            } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println();

        getRoster().addRosterListener(this);
    }

    @Override
    public void newIncomingMessage(EntityBareJid fromJabberId, Message message, Chat chat) {
        super.newIncomingMessage(fromJabberId, message, chat);

        if(message.getBody().equalsIgnoreCase("Estou aguardando sim")) {
            //Define o ID do cliente que será atendido, que indica que o funcinário agora está ocupado.
            setDestinationUser(fromJabberId);
            sendMessage(chat, "Em que posso ajudá-lo?");
        }
    }

    @Override
    public void presenceChanged(Presence presence) {
        //Se um cliente conectou e o funcionário não está conversando com ninguém
        if(presence.isAvailable() && !isChatting()){
            this.setDestinationUser(presence.getFrom().asEntityBareJidIfPossible());

            //Pergunta ao cliente se ele ainda está aguardando
            Chat chat = getChatManager().chatWith(presence.getFrom().asEntityBareJidIfPossible());
            sendMessage(chat, "Está aguardando?");
        }
        // Se o cliente que estávamos atendendo desconectar, então...
        else if(presence.getType() == Presence.Type.unavailable && presence.getFrom().asEntityBareJidIfPossible().equals(getDestinationUser())){
            /*
            ...define o nome do usuário de destino (com quem o funcionário estava conversado)
            para null. Assim, o funcionário vai ficar livre para atender outro cliente. */
            System.out.print("\nCliente " + presence.getFrom() + " desconectou.");
            setDestinationUser(null);
        }
    }
}