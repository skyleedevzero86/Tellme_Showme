import styles from './home.module.css';

export default function HomePage() {
  return (
    <main className={styles.main}>
      <h1 className={styles.title}>Tellme Showme</h1>
      <p className={styles.desc}>
        텔레그램 Bot API 예제 (웹후크, Long Polling, 채널 브로드캐스트, 수신 이력)
      </p>
      <p className={styles.hint}>상단 메뉴에서 기능을 선택하세요.</p>
    </main>
  );
}
