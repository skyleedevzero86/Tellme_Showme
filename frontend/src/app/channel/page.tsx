import { ChannelClient } from './ChannelClient';

export default function ChannelPage() {
  return (
    <div className="chat-layout">
      <div className="chat-container">
        <header className="chat-header">
          채널 브로드캐스트
          <span className="chat-header-sub">
            설정한 채널로 메시지·이미지·파일을 전송합니다.
          </span>
        </header>
        <div className="chat-messages">
          <div className="section">
            <ul className="muted" style={{ paddingLeft: 18 }}>
              <li>application.yml의 `telegram.channel-username` 값이 필요합니다.</li>
              <li>채널에 봇을 관리자로 추가한 뒤 @채널이름으로 설정합니다.</li>
            </ul>
          </div>
          <hr />
          <ChannelClient />
        </div>
      </div>
    </div>
  );
}
