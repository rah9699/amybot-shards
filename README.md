# amybot shards

The magical thing that dumps Discord events into the backend. :tm:

![Powered by Shreddo](https://img.shields.io/badge/Powered%20by-Shreddo-FF69B4.svg)

Much thanks to [Shredder121](https://github.com/shredder121) for yelling at me when I make dumb decisions, and for generally being awesome. 

Due to some needs of mine, this is also a Discord API library, of sorts. Eventually I'll be able to replace JDA, but not any time soon

## Badges

[![forthebadge](http://forthebadge.com/images/badges/uses-badges.svg)](http://forthebadge.com) 
[![forthebadge](http://forthebadge.com/images/badges/made-with-crayons.svg)](http://forthebadge.com)
[![forthebadge](http://forthebadge.com/images/badges/built-with-love.svg)](http://forthebadge.com)
[![forthebadge](http://forthebadge.com/images/badges/compatibility-pc-load-letter.svg)](http://forthebadge.com)
[![forthebadge](http://forthebadge.com/images/badges/contains-technical-debt.svg)](http://forthebadge.com)
[![forthebadge](http://forthebadge.com/images/badges/powered-by-electricity.svg)](http://forthebadge.com)


## Current TODO list

- Finish mechanisms for building caches out of Discord events
- Make sure that snowflake ZSET caches are actually updated
- Make sure that tests actually pass
- Abstract out the messenger layer a bit better so that I can drop in RMQ support later

## How is everything connected?

This simple graph should explain it all quite nicely

![Graph](simple.svg)

The key takeaway here is that Graphviz is hard to do right. :thumbsup:

## wtf is wrong with you

Well JDA didn't support what I wanted, and external caching + raw mWS event access is nice. 

## Configuration

Configuration is done through environment variables.

```bash
# The token for your bot
BOT_TOKEN="no default provided, obviously"
# Your redis host address. Default is 'redis'
REDIS_HOST="redis"
# The password to your redis host. This is a requirement
REDIS_PASS="a"
# How to derive shard id / scale. Default method is the Rancher metadata service, but may also be configured through environment variables
# Possible values: "rancher", "env"
SHARDING_METHOD="rancher"
# Used when SHARDING_METHOD="env"
SHARD_ID=15
# Used when SHARDING_METHOD="env"
SHARD_SCALE=27
```

## Other

### Some thoughts on caching

- **Redisson is not used anymore.** While unfortunate, this decision was made because Redisson was just like "lol what is serializing correctly :S" and decided to blow it up using the Jackson codec. 
- All guilds have their own object in the cache
- All users have their own object in the cache
- All members lists are a part of their respective guild objects, as a set of Member objects
- Member objects reference their user object by snowflake
- Building initial caches is pretty heavily abusive to the redis server; make sure you're actually ready for this. 

### Caching format

#### Single objects

Guild: `guild:snowflake:bucket`

User: `user:snowflake:bucket`

Member: `member:guild_snowflake:user_snowflake:bucket`

Channel: `channel:snowflake:bucket`

Role: `role:guild_snowflake:snowflake:bucket`

Emote: `emote:guild_snowflake:snowflake:bucket`

#### Snowflake sets

Users: `user:sset`

Guilds: `guild:sset`

Members: `member:guild_snowflake:sset`?

Channels: `channel:sset`

Roles: `role:sset`

Emotes: `emote:sset`