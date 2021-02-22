from json.decoder import JSONDecodeError
import redis
import logging
import random
import time
import json
import sys
from typing import Final

from redis.client import PubSub, Redis                # ~const non proprio const, serve Python 3.8

"""
Questo master pubblica task nel dataspace Redis, il suo compito è:
    1) generare un task
    2) inserirlo nella lista dei task da svolgere
    3) eventualmente ritrovarlo nella lista dei task svolti
    5) Ripetere dopo dt
"""

#HOST : Final = "localhost" 
HOST : Final = "192.168.1.17" 
PORT : Final = 6379
DB : Final = 0
MASTER_NAME : Final = f"master{random.randint(0, 1e4)}_"
TODO_TASK_LIST : Final = "todo_tasks"
DONE_TASK_LIST : Final = "done_tasks"
TASK_PER_SECOND : Final = 1/0.5   #due task al secondo


format = "%(asctime)s: %(message)s"
logging.basicConfig(format=format, level=logging.INFO, datefmt="%H:%M:%S")

pool = redis.ConnectionPool(host=HOST, port=PORT)
taskTypes = ["task+", "task-", "task*", "task/"]
only_sum = False
#taskTypes = ["task+"]
task_counter = 1
agent_set = set()

class Task:
    def __init__(self):
        self.task_type = taskTypes[random.randint(0,len(taskTypes)-1)]
        self.task_name = f"{MASTER_NAME}{self.task_type}_{task_counter}"
        self.a = random.randint(1, 100)
        self.b = random.randint(1, 100)
        self.result = 0
        self.lock = "lock_"+self.task_name    # lo imposto come identificativo del task

    def __repr__(self) -> str:
        return self.task_name

def write_stat_agents() -> None:
    r = redis.Redis(connection_pool=pool)
    f = open(f"masters_stats_auth/stats.txt", "w+")     #se voglio le stat die task per ogni master, devo creare i dati nel master 
    string = ""
    for e in agent_set:
        string += f'{e} has accomplished {r.get(e).decode("utf-8")} tasks\n'
    f.write(string)
    f.close()

def show_task_lists(r:Redis) -> None:
    f = open(f'proactive_stats/stats.txt', "w+")
    todo = []
    todo = r.lrange(name=TODO_TASK_LIST, start=0, end=-1)
    done = []
    done = r.lrange(name=DONE_TASK_LIST, start=0, end=-1)
    string = f'To-do list:\n'
    for i in todo:
        string += f'{i}\n'
    string += f'\nDone list:\n'
    for i in done:
        string += f'{i}\n'
    f.write(string)
    f.close()

if __name__ == "__main__":
    try:
        r = redis.Redis(connection_pool=pool) 
        p = r.pubsub()
        p.subscribe("to-master")
    except Exception:
        sys.exit(f"Error: No connection to Redis server on socket: ({HOST}; {PORT})\nActivate one or choose an available socket")

    logging.info(f"Start pushing tasks, {MASTER_NAME}")
    while True:        
        active_task = Task()

        #pubblico task-----------------------------------------------------------------------------------------------------------
        logging.info(f"Insert task into todo-list {active_task.task_name}\n{active_task.__dict__}")
        r.hset(name=active_task.task_name, mapping=active_task.__dict__)    #inserisco hash
        r.lpush(TODO_TASK_LIST, active_task.task_name)            #aggiungo alla lista il task

        #show_task_lists(r)

        task_counter += 1
        time.sleep(TASK_PER_SECOND)