# Jedis

Jedis is a blazingly small and sane redis java client.

Jedis was conceived to be EASY to use and FULLY COMPATIBLE with the latest version of redis.

Jedis is a WORK IN PROGRESS.

## What's still missing?
- Sorting
- Publish/Subscribe
- Persistence control commands
- Remote server control commands
- The AUTH, SORT, BLPOP, BRPOP, ZRANGEBYSCORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZUNIONSTORE, ZINTERSTORE commands

But stay close because things are going fast and all this will be implemented soon!

## Ok.. so what's already done?
- Connection handling (not AUTH)
- Commands operating on all the kind of values
- Commands operating on string values
- Commands operating on hashes
- Commands operating on lists (not SORT, BLPOP, BRPOP)
- Commands operating on sets
- Commands operating on sorted sets (not SORT, ZRANGEBYSCORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZUNIONSTORE, ZINTERSTORE)
- Transactions

## How do I use it?

You can download the latests build at: 
    http://github.com/xetorthio/jedis/downloads

To use it just:
    
    Jedis jedis = new Jedis("localhost");
    jedis.connect();
    jedis.set("foo", "bar");
    String value = jedis.get("foo");

And you are done!

License
-------

Copyright (c) 2010 Jonathan Leibiusky

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

