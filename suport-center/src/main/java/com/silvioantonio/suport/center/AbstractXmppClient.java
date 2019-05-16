package com.silvioantonio.suport.center;

import java.io.IOException;
import java.util.Scanner;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.roster.AbstractRosterListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;


public abstract class AbstractXmppClient extends AbstractRosterListener implements AutoCloseable, IncomingChatMessageListener {
    
    private Scanner scanner;
    private XMPPTCPConnectionConfiguration.Builder configBuilder;
    private Roster roster;
    private XMPPTCPConnection connection;
    private ChatManager chatManager;
    private String username;
    private String password;
    private String msg;
    private String domain;
    private EntityBareJid destinationUser;
    
    public AbstractXmppClient() {
        scanner = new Scanner(System.in);
        configBuilder = XMPPTCPConnectionConfiguration.builder();
    }

    protected void validateUsernameAndDomain(){
        
        if(username.matches(".*@.*")) {
            
            int i = username.indexOf("@");

            domain = username.substring(i+1);

            username = username.substring(0, i);
            System.out.println("username:"+username);
            System.out.println("domain:"+domain);
            return;
        }

        throw new IllegalArgumentException("Nome de usuário deve estar no formato nome@dominio");
    }

    @Override
    public void close() {
        connection.disconnect();
    }

    public Scanner getScanner() {
        return scanner;
    }

    public XMPPTCPConnection getConnection() {
        return connection;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public String getUsername() {
        return username;
    }

    public String getJabberId(){
        return username + "@" + domain;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getDomain() {
        return domain;
    }

    public EntityBareJid getDestinationUser() {
        return destinationUser;
    }

    public synchronized void setDestinationUser(final EntityBareJid destinationUser) {
        this.destinationUser = destinationUser;
    }

    protected boolean start(){
        System.out.print("Digite seu login (usuario@dominio):");
        username = scanner.nextLine();
        
        validateUsernameAndDomain();

        System.out.print("Digite sua senha: ");
        password = scanner.nextLine();

        if(!connect()){
            System.out.println("Erro aqui");
            return false;
        }

        afterConnected();

        sendMessageLoop();
        return true;
    }

    private boolean connect() {
        try {
            configBuilder
                    .setUsernameAndPassword("testesilvio001", "01477410")
                    .setResource("desktop")
                    .setXmppDomain("xabber.org")
                    .setHost("xabber.org");

            connection = new XMPPTCPConnection(configBuilder.build());
            connection.setReplyTimeout(5000);
            //System.out.println(connection.getStreamId());
            
            try{
                connection.connect();
            }catch(SmackException | IOException | XMPPException | InterruptedException xmppe){
                System.err.println("Erro no level de protocolo: "+xmppe);
            }
            
            chatManager = ChatManager.getInstanceFor(connection);
            //chatManager.addIncomingListener(this);

            System.out.println("Conectado com sucesso no servidor XMPP");
            
        } catch (IOException e) {
            System.out.println("Erro ao conectar ao servidor: "+e.getMessage());
            return false;
        }
        
        roster = Roster.getInstanceFor(connection);

        Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);

        return login();
    }

    protected void afterConnected(){
    }

    private boolean login() {
        try {
            connection.login();
            return true;
        } catch (XMPPException | SmackException | InterruptedException | IOException e) {
            System.out.println("Erro ao efetuar login. Verifique suas credenciais: "+e.getMessage());
        }

        return false;
    }

    @Override
    public void newIncomingMessage(EntityBareJid fromJabberId, Message message, Chat chat){
        System.out.println("\n" + fromJabberId + " diz: " + message.getBody());
    }

    protected void sendMessageLoop() {
        while(!isChatting()){
            /*Aguarda inicio de conversa com outra pessoa.
            * O cliente aguarda por um atendente e o atendente aguarda
            * até um cliente solicitar atendimento.*/
        }

        while(true){
            System.out.print("\nDigite uma mensagem (ou 'sair' para fechar): ");
            msg = scanner.nextLine();
            if (msg.equalsIgnoreCase("sair")) {
                break;
            }

            if(sendMessage(msg, destinationUser)) {
                System.out.println("Mensagem enviada para " + destinationUser);
            }
        }
    }

    /**
     * Envia uma mensagem para um determinado usuário
     * @param msg texto da mensagem a ser enviada
     * @param destinationUser ID do usuário para quem deseja-se enviar a mensagem (nome de usuário/login)
     * @return 
     */
    protected boolean sendMessage(final String msg, final EntityBareJid destinationUser){
        if (connection.isConnected()) {
            try {
                chatManager.chatWith(destinationUser).send(msg);
                return true;
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                System.err.println("Erro ao enviar mensagem: " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Envia uma mensagem usando um objeto {@link Chat} previamente criado.
     * @param chat o objeto {@link Chat} que representa uma conversa com um usuário de destino
     * @param msg a mensagem a ser enviada
     */
    protected void sendMessage(final Chat chat, final String msg){
        try {
            chat.send(msg);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            System.err.println("Não foi possível enviar a mensagem: " + e.getMessage());
        }
    }

    public synchronized boolean isChatting() {
        return destinationUser != null;
    }

    public Roster getRoster() {
        return roster;
    }
}
