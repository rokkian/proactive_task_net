from json.decoder import JSONDecodeError
import redis
from redis import Redis
import logging
import random
import time
import json
import sys
from typing import Final

HOST : Final = "192.168.1.17" #"localhost"
PORT : Final = 6379
DB : Final = 0
MASTER_NAME : Final = f"master{random.randint(0, 1e4)}_"
TODO_TASK_LIST : Final = "todo_tasks"
DONE_TASK_LIST : Final = "done_tasks"

format = "%(asctime)s: %(message)s"
logging.basicConfig(format=format, level=logging.INFO, datefmt="%H:%M:%S")

pool = redis.ConnectionPool(host=HOST, port=PORT)
taskTypes = ["task+", "task-", "task*", "task/"]
only_sum = False
#taskTypes = ["task+"]
task_counter = 0
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
    f = open(f'stats.txt', "w+")
    todo = []
    todo = r.lrange(name=TODO_TASK_LIST, start=0, end=-1)
    done = []
    done = r.lrange(name=DONE_TASK_LIST, start=0, end=-1)
    string = f'To-do list:\n'
    for i in todo:
        string += f'{i}\n'
    string += f'\nDone list:\n'
    for i in done:
        string += f'"{i.decode("utf-8")}" , svolta da: "{r.hget(i, "executedBy").decode("utf-8")}"\n'
    f.write(string)
    f.close()

if __name__=='__main__':
    r = redis.Redis(connection_pool=pool)

    while True:
        show_task_lists(r)
        time.sleep(1)
