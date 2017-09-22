package chat.amy;

import chat.amy.jda.WrappedEvent;
import chat.amy.message.EventMessenger;
import chat.amy.message.RedisMessenger;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author amy
 * @since 9/2/17.
 */
@SuppressWarnings({"unused", "UnnecessarilyQualifiedInnerClassAccess", "WeakerAccess", "FieldCanBeLocal"})
public final class AmybotShard {
    @Getter
    private static final EventBus eventBus = new EventBus();
    @Getter
    @SuppressWarnings("TypeMayBeWeakened")
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .build();
    @Getter
    private final Logger logger = LoggerFactory.getLogger("amybot-shard");
    // TODO: Make this configurable or smth
    private final EventMessenger messenger = new RedisMessenger();
    @Getter
    private JDA jda;
    private int shardId;
    private int shardScale;
    
    private AmybotShard() {
        getLogger().info("Starting up new amybot shard...");
    }
    
    public static void main(final String[] args) {
        new AmybotShard().start();
    }
    
    private void start() {
        /*
         * Order of things is something like:
         * - Start container
         * - Derive shard ID from metadata
         * - Set up send / recv. queues
         * - Actually boot shard
         */
        eventBus.register(this);
        eventBus.post(InternalEvent.GET_SHARD_ID);
    }
    
    @Subscribe
    @SuppressWarnings("ConstantConditions")
    public void getShardId(final InternalEvent event) {
        if(event == InternalEvent.GET_SHARD_ID) {
            // TODO: Make generic shard ID derivation thingie for those who wanna use other things
            getLogger().info("Deriving shard numbers from Rancher...");
            try {
                final String serviceIndex = client.newCall(new Request.Builder()
                        .url("http://rancher-metadata/2015-12-19/self/container/service_index")
                        .build()).execute().body().string();
                final String serviceName = client.newCall(new Request.Builder()
                        .url("http://rancher-metadata/2015-12-19/self/container/service_name")
                        .build()).execute().body().string();
                final String serviceScale = client.newCall(new Request.Builder()
                        .url(String.format("http://rancher-metadata/2015-12-19/services/%s/scale", serviceName))
                        .build()).execute().body().string();
                // 12 containers -> 0 - 11 IDs
                shardId = Integer.parseInt(serviceIndex) - 1;
                shardScale = Integer.parseInt(serviceScale);
                eventBus.post(InternalEvent.START_BOT);
            } catch(final IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Subscribe
    public void handleDiscordEvent(@Nonnull final WrappedEvent event) {
        logger.debug("Queued wrapped " + event.getType() + " event.");
        messenger.queue(event);
    }
    
    @Subscribe
    public void startBot(final InternalEvent ievent) {
        if(ievent == InternalEvent.START_BOT) {
            try {
                // TODO: Poll token bucket until we're allowed to connect
                // TODO: Build networked SessionReconnectQueue(?)
                jda = new JDABuilder(AccountType.BOT)
                        .useSharding(shardId, shardScale)
                        .setToken(System.getenv("BOT_TOKEN"))
                        .addEventListener((EventListener) event -> {
                            if(event instanceof ReadyEvent) {
                                // TODO: Probably wanna give people another way to set this
                                jda.getPresence().setGame(Game.of(jda.getSelfUser().getName() + " shard " + shardId + " / " + shardScale));
                                getLogger().info("Logged in as shard " + shardId + " / " + shardScale);
                                eventBus.post(InternalEvent.READY);
                            }
                        })
                        .buildAsync();
            } catch(final LoginException | RateLimitedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public enum InternalEvent {
        GET_SHARD_ID,
        START_BOT,
        READY,
    }
}
