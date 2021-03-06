package chat.amy.cache.guild;

import chat.amy.cache.Snowflake;
import chat.amy.cache.raw.RawMember;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author amy
 * @since 9/24/17.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class Member implements Snowflake {
    private String id;
    private String nickname;
    private List<String> roles;
    @JsonProperty("joined_at")
    private String joinedAt;
    private boolean deaf;
    private boolean mute;
    
    private Member() {
    }
    
    public static Member fromRaw(final RawMember r) {
        final Member m = new Member();
        m.id = r.getUser().getId();
        m.nickname = r.getNick();
        m.roles = r.getRoles();
        m.joinedAt = r.getJoinedAt();
        m.deaf = r.isDeaf();
        m.mute = r.isMute();
        return m;
    }
}
