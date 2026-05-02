import com.familie.cheltuieli_familie.service.ThePipeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ThePipeHandler thePipeHandler;
    private final com.familie.cheltuieli_familie.security.interceptor.SessionHandshakeInterceptor sessionHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Înregistrăm "The Pipe" la endpoint-ul /locatie
        registry.addHandler(thePipeHandler, "/locatie")
                .addInterceptors(sessionHandshakeInterceptor)
                .setAllowedOriginPatterns("*"); // Mai robust decat setAllowedOrigins pentru handshakes
    }
}
