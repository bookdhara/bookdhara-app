package com.bookdhara.safety;

import com.bookdhara.common.ApiException;
import com.bookdhara.user.User;
import com.bookdhara.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Blocking works both ways: neither person sees the other's listings, can buy from them or message
 * them. Orders already placed stay, so either side can still finish or cancel them.
 */
@Service
public class BlockService {

    public record BlockedUser(Long id, String name, Instant blockedAt) {
    }

    private final UserBlockRepository blocks;
    private final UserRepository users;
    private final Clock clock;

    public BlockService(UserBlockRepository blocks, UserRepository users, Clock clock) {
        this.blocks = blocks;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void block(Long me, Long other) {
        if (me.equals(other)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BLOCK_SELF", "You can't block yourself.");
        }
        if (!users.existsById(other)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
        if (!blocks.existsByBlockerIdAndBlockedId(me, other)) {
            blocks.save(new UserBlock(me, other, clock.instant()));
        }
    }

    @Transactional
    public void unblock(Long me, Long other) {
        blocks.unblock(me, other);
    }

    @Transactional(readOnly = true)
    public List<BlockedUser> blockedBy(Long me) {
        List<UserBlock> list = blocks.findByBlockerIdOrderByCreatedAtDesc(me);
        Map<Long, User> byId = users.findAllById(list.stream().map(UserBlock::getBlockedId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return list.stream()
                .map(b -> new BlockedUser(b.getBlockedId(),
                        byId.containsKey(b.getBlockedId()) ? byId.get(b.getBlockedId()).getName() : null, b.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean blockedEitherWay(Long a, Long b) {
        return blocks.existsBetween(a, b);
    }

    /** For actions that must stop between blocked people. */
    public void requireNotBlocked(Long a, Long b, String message) {
        if (blockedEitherWay(a, b)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "BLOCKED", message);
        }
    }
}
