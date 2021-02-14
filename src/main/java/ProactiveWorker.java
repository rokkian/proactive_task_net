import redis.clients.jedis.*;
/* Agente proattivo:
 * 	1) Legge il dataspace in cerca dei task
 *  2) Per ogni task trovato attiva un thread di svolgimento
 *  3) Attende notifiche di aggiunta task alla lista Redis
 *  4) Torna a leggere il dataspace in cerca di task
 *  5) Attiva i thread di svolgimento per i task trovati
 * 
 * */

public class ProactiveWorker {
	static final String REDIS_HOST = "192.168.1.17";//"localhost";
	static final Integer REDIS_PORT = 6379;
	static final int DB = 0;
	static final String IdAgente = "Agent"+String.valueOf((int)(Math.random()*10000));		//va scelto un identificativo del singolo agente slave
	static final boolean VERBOSE = true;
	static final String TASK_LIST = "todo_tasks";
	static final String DONE_LIST = "done_tasks";
	static final int LOCK_DURATION = 12; //durata dei lock in secondi
	static final long MAX_TASK_DURATION = 1;	//massima durata di un task in secondi
	
	public static void main(String[] args) {
		final JedisPool pool = new JedisPool(REDIS_HOST, REDIS_PORT);
		Jedis redis = pool.getResource();
		System.out.println("Start Slave!, "+IdAgente);
		final RedisAPI redisAPI = new RedisAPI(pool, LOCK_DURATION, IdAgente, TASK_LIST, DONE_LIST);
		DataspaceQuery dataspaceQuery = new DataspaceQuery(IdAgente, MAX_TASK_DURATION, redisAPI);

		//passo per la lista dei task
		try {
			dataspaceQuery.run();
			
		} catch (Exception e) {
			System.err.println("Primo check della lista task fallito");
			e.printStackTrace();
		}

		//aspetto notifiche per le passate successive
		redis.psubscribe( new JedisPubSub() {
			@Override
	        public void onPMessage(final String pattern, final String channel, final String message) {
				if (message.equalsIgnoreCase("lpush") || message.equalsIgnoreCase("rpush"))
					
					try {
						new DataspaceQuery(IdAgente, MAX_TASK_DURATION, redisAPI).run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				
	        }
		}, "__keysp*"+TASK_LIST);
		
		redis.close();
		pool.close();
	}
}
