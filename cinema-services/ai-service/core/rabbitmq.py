import json
import logging
import threading
import time
import pika
from app.config import get_settings
from events.payment_event_consumer import handle_payment_succeeded_event
from events.catalog_event_consumer import handle_movie_published_event

logger = logging.getLogger(__name__)

_consumer_thread = None
_should_run = True


def _run_consumer():
    settings = get_settings()
    credentials = pika.PlainCredentials(settings.RABBITMQ_USER, settings.RABBITMQ_PASS)
    parameters = pika.ConnectionParameters(
        host=settings.RABBITMQ_HOST,
        port=settings.RABBITMQ_PORT,
        credentials=credentials,
        heartbeat=60
    )

    while _should_run:
        try:
            logger.info(f"Connecting to RabbitMQ broker at {settings.RABBITMQ_HOST}:{settings.RABBITMQ_PORT}...")
            connection = pika.BlockingConnection(parameters)
            channel = connection.channel()

            # Declare Payment Events exchange and queue
            payment_exchange = "cinema.payment.events"
            payment_queue = "ai-service.payment.events.queue"
            channel.exchange_declare(exchange=payment_exchange, exchange_type="topic", durable=True)
            channel.queue_declare(queue=payment_queue, durable=True)
            channel.queue_bind(exchange=payment_exchange, queue=payment_queue, routing_key="payment.succeeded")

            # Declare Catalog Events exchange and queue
            catalog_exchange = "cinema.catalog.events"
            catalog_queue = "ai-service.catalog.events.queue"
            channel.exchange_declare(exchange=catalog_exchange, exchange_type="topic", durable=True)
            channel.queue_declare(queue=catalog_queue, durable=True)
            channel.queue_bind(exchange=catalog_exchange, queue=catalog_queue, routing_key="movie.published")
            channel.queue_bind(exchange=catalog_exchange, queue=catalog_queue, routing_key="movie.updated")

            logger.info("RabbitMQ Consumer is ready and listening for events.")

            def on_message(ch, method, properties, body):
                try:
                    data = json.loads(body.decode("utf-8"))
                    routing_key = method.routing_key
                    if routing_key == "payment.succeeded":
                        handle_payment_succeeded_event(data)
                    elif routing_key.startswith("movie."):
                        handle_movie_published_event(data)
                    ch.basic_ack(delivery_tag=method.delivery_tag)
                except Exception as ex:
                    logger.error(f"Error processing RabbitMQ message: {ex}", exc_info=True)
                    ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

            channel.basic_consume(queue=payment_queue, on_message_callback=on_message)
            channel.basic_consume(queue=catalog_queue, on_message_callback=on_message)
            channel.start_consuming()

        except Exception as e:
            if not _should_run:
                break
            logger.warning(f"RabbitMQ connection lost ({e}). Retrying in 10 seconds...")
            time.sleep(10)


def start_rabbitmq_consumer():
    global _consumer_thread, _should_run
    _should_run = True
    if _consumer_thread is None or not _consumer_thread.is_alive():
        _consumer_thread = threading.Thread(target=_run_consumer, daemon=True)
        _consumer_thread.start()
        logger.info("Started background RabbitMQ consumer thread.")


def stop_rabbitmq_consumer():
    global _should_run
    _should_run = False
    logger.info("Stopped RabbitMQ consumer.")
