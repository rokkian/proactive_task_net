# proactive_task_net

This is an implementation of a Contract Net, where a variable set of proactive workers checks the shared dataspace (Redis) in search of task to do.
There are multiple publishers of tasks that can be activated simultaneously and the the workers or publisherse of tasks can be effimeral.
The time-decoupling between publishers and executioners of tasks is reached.

The system is process crash resistant if a maximum duration time of the tasks is known a-priori. Otherways those fault can loose the currently ongoing task.

The coordination is implemented through a mutual exclusion lock in Redis. If needed on a distrubuted implementation of Redis it has to be implemented adding the Redlock algorithm support (https://redis.io/topics/distlock).

The publishers of task are simple Python3 scripts who add tasks to a list of to-do task on the Redis DB.

The worker it's a Java-Maven simple agent. Further applications could implement a selection of the type of task execute if the agent is supposed to have only limited capabilities.
