package ru.anatomica.cloud_storage.Handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import ru.anatomica.cloud_storage.Controller.Callback;
import ru.anatomica.cloud_storage.MainActivity;
import org.apache.log4j.Logger;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    static Logger log = Logger.getLogger("AuthHandler");
    private Callback authOkCallback;
    private MainActivity mainActivity;
    private int authOk = 0;
    private int regOk = 0;

    public AuthHandler(Callback authOkCallback, MainActivity mainActivity) {
        this.authOkCallback = authOkCallback;
        this.mainActivity = mainActivity;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) return;
        ByteBuf buf = ((ByteBuf) msg);
        if ((buf.readableBytes() > (byte) 6) && authOk == 1) {
            ctx.fireChannelRead(buf);
        } else {
            byte data = buf.readByte();
            if (data == (byte) 2) {
                log.info("STATE: Verification is Not Successful!");
                messageToService("Вы ввели неверное имя пользователя или пароль!");
            }
            if (data == (byte) 3) {
                log.info("STATE: Verification is Successful!");
                buf.release();
                authOk = 1;
                authOkCallback.callBack();
                ctx.pipeline().remove(this);
            }
            if (data == (byte) 4) {
                log.info("STATE: Registration is Not Successful!");
                messageToService("Данный Логин занят! \nПожалуйста, выберите другой Логин!");
            }
            if (data == (byte) 5) {
                log.info("STATE: Registration is Successful!");
                messageToService("Вы зарегистрированы! Осуществляется выход!\nПожалуйста, войдите в приложение заново!");
                buf.release();
                regOk = 1;
                mainActivity.logoutAfterReg();
            }
        }
    }

    private void messageToService(String message) {
        mainActivity.runOnUiThread(() -> mainActivity.serviceMessage(message));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
