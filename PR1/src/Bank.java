import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Bank {
    private static final int ATM_COUNT = 4; // Кількість доступних банкоматів
    private static final int WORKING_HOURS = 8; // Час роботи банку в секундах
    private static Semaphore atmSemaphore = new Semaphore(ATM_COUNT); // Семафор для контролю доступу до банкоматів
    private static boolean isBankOpen = true; // Стан банку

    public static void main(String[] args) throws InterruptedException {
        // Створення кількох клієнтських потоків
        for (int i = 1; i <= 24; i++) {
            new Thread(new Client(i)).start();
        }

        // Симуляція робочих годин банку
        TimeUnit.SECONDS.sleep(WORKING_HOURS);
        closeBank();
    }

    // Метод для закриття банку після робочих годин
    public static void closeBank() {
        isBankOpen = false;
        System.out.println("Банк закритий. Більше немає доступу до банкоматів.");
    }

    // Клас Client реалізує Runnable для створення клієнтських потоків
    static class Client implements Runnable {
        private int clientId;
        private boolean sessionActive = true; // Відстеження активності сесії

        public Client(int clientId) {
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                while (sessionActive) {
                    if (atmSemaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                        synchronized (Bank.class) { // Синхронізація для правильного порядку повідомлень
                            if (!isBankOpen) {
                                System.out.println("Клієнт " + clientId + " йде, бо банк закритий.");
                                atmSemaphore.release(); // Випуск банкомату та вихід
                                sessionActive = false;
                                break;
                            }
                            System.out.println("Клієнт " + clientId + " користується банкоматом.");
                        }

                        // Симуляція використання банкомату з регулярними перевірками стану банку
                        for (int i = 0; i < 4; i++) { // Симуляція 2-секундної задачі з перевірками через 0.5 с
                            TimeUnit.MILLISECONDS.sleep(500);
                            if (!isBankOpen) {
                                synchronized (Bank.class) { // Синхронізація для обробки повідомлень
                                    System.out.println("Сесію клієнта " + clientId + " перервано, оскільки банк закрився.");
                                    atmSemaphore.release(); // Випуск банкомату та вихід
                                }
                                sessionActive = false;
                                return; // Завершення сесії, якщо банк закритий
                            }
                        }
                        synchronized (Bank.class) { // Гарантуємо правильну послідовність
                            System.out.println("Клієнт " + clientId + " завершив користування банкоматом.");
                            atmSemaphore.release(); // Випуск банкомату для наступного клієнта
                        }
                        sessionActive = false; // Клієнт завершує успішно, більше спроб не потрібно
                    } else if (!isBankOpen) {
                        // Банк закритий, клієнт не скористається банкоматом
                        synchronized (Bank.class) {
                            System.out.println("Клієнт " + clientId + " виявляє, що банк закритий і йде.");
                        }
                        sessionActive = false; // Більше спроб, оскільки банк закритий
                        break;
                    } else {
                        // Банк відкритий, але банкомат недоступний
                        System.out.println("Клієнт " + clientId + " чекає на банкомат.");
                        TimeUnit.MILLISECONDS.sleep(500); // Невелика затримка перед повторною спробою
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Клієнт " + clientId + " був перерваний.");
            }
        }
    }
}
