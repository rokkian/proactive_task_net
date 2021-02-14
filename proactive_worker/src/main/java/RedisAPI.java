import java.util.List;
import java.util.Map;

import redis.clients.jedis.*;
//import redis.clients.jedis.params.SetParams;

/* Questa classe ha lo scopo di accedere a Redis in modo esclusivo e ordinato
 * 
 * */

public class RedisAPI {
	JedisPool pool;
	String TASK_LIST_NAME;
	String DONE_LIST_NAME;
	String IdAgente;
	int LOCK_DURATION;
	
	public RedisAPI(JedisPool pool, int LOCK_DURATION, String IdAgente, String task_list, String done_list) {
		// TODO Auto-generated constructor stub
		this.pool = pool;
		this.IdAgente = IdAgente;
		this.TASK_LIST_NAME = task_list;
		this.DONE_LIST_NAME = done_list;
		this.LOCK_DURATION = LOCK_DURATION;
	}
	
	public synchronized List<String> readTaskList() {
		Jedis redis = pool.getResource();
		List<String> taskList = redis.lrange(TASK_LIST_NAME, 0, -1);
		redis.close();
		return taskList;
	}
	
	public synchronized long setLock(String lock_name, boolean lock_stales) { //con TTL 0 il lock non scade
		Jedis redis = pool.getResource();
		long res = redis.setnx(lock_name, this.IdAgente);
		
		if(res == 1 && lock_stales) {	//se acuquisco il lock imposto il tempo di vita
			redis.setex(lock_name, this.LOCK_DURATION, this.IdAgente);	}
		
		redis.close();
		return res;
	}
	
	public synchronized Map<String, String> getHash(String TASK_NAME) {
		Jedis redis = pool.getResource();
		Map<String, String> taskMap = redis.hgetAll(TASK_NAME);
		
		redis.close();
		return taskMap;
	}
	
	public synchronized void outputTransaction(String TASK_NAME, double result) {
		Jedis redis = this.pool.getResource();
		String lockTaskName = "lock_"+TASK_NAME;
		
		try {
			Transaction t = redis.multi();			//transazione atomica
			t.hset(TASK_NAME, "result", String.valueOf(result));	//aggiungo il risultato all'hash del task
			t.hsetnx(TASK_NAME, "executedBy", this.IdAgente);		//aggiungo se non esiste già (e non deve) il nome del worker che ha svolto il task
			t.lrem(TASK_LIST_NAME, 0, TASK_NAME);			//tolgo il task dalla lista todo
			//t.zadd(DONE_LIST_NAME, 0, TASK_NAME);				//aggiungo il task alla lista done
			t.lpush(DONE_LIST_NAME, TASK_NAME);
			t.setex(lockTaskName, 5, this.IdAgente);	//elimino il lock dopo tot secondi
			t.incr(IdAgente);						//incremento la conta dei task svolti dall'agente
			t.exec();								//eseguo transazione
			t.close();
		} catch (Exception e) {
			System.err.println("Transazione fallita");
			e.printStackTrace();
		}
		redis.close();
	}
	
	public synchronized int agentCountTasks() {
		Jedis redis = pool.getResource();
		int count = Integer.parseInt(redis.get(this.IdAgente));
		redis.close();
		return count;
	}
}