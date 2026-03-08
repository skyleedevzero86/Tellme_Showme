import { ChannelBroadcastPanel } from '@/presentation/components/ChannelBroadcastPanel';

export default function ChannelPage() {
  return (
    <>
      <h2>텔레그램 채널 브로드캐스팅 예제</h2>
      <hr />
      <ul>
        <li>설정된 채널(application.yml의 telegram.channel-username)에 브로드캐스팅합니다.</li>
        <li>채널에 봇을 관리자로 추가한 뒤 @채널이름으로 설정합니다.</li>
      </ul>
      <br />
      <ChannelBroadcastPanel />
    </>
  );
}
