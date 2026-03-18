import Link from 'next/link';
import { ChannelClient } from './ChannelClient';

export default function ChannelPage() {
  return (
    <>
      <h2>텔레그램 채널 브로드캐스팅 예제</h2>
      <hr />
      <ul>
        <li>설정된 채널(application.yml의 telegram.channel-username)에 브로드캐스팅합니다.</li>
        <li>채널에 봇을 관리자로 추가한 뒤 @채널이름으로 설정합니다.</li>
        <li>
          <Link href="/channel/history">받은 메시지 이력</Link>에서 텔레그램으로 수신한 메시지·파일을 페이징·검색할 수 있습니다.
        </li>
      </ul>
      <br />
      <ChannelClient />
    </>
  );
}
