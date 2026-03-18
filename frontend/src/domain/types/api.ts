export type WebhookResponse = {
  ok?: boolean;
  description?: string;
  result?: unknown;
};

export type WebhookInfoResult = {
  url?: string;
  pending_update_count?: number;
  last_error_message?: string;
  last_error_date?: number;
};

export type WebhookInfoResponse = {
  ok?: boolean;
  result?: WebhookInfoResult | null;
};

export type SendMessageResponse = {
  status: string;
  message: string | null;
};

export type GetUpdatesResponse = {
  result: string;
  webhookActive?: boolean;
};

export type MessageHistoryItem = {
  id: number;
  telegramMessageId: number;
  chatId: string;
  fromUserId: string;
  fromUserName: string | null;
  text: string | null;
  receivedAt: string;
  createdAt: string;
};

export type MessageHistoryResponse = {
  status: number;
  msg: string;
  data: {
    content: MessageHistoryItem[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  } | null;
};

export type FileHistoryItem = {
  id: number;
  fileName: string;
  objectKey: string;
  contentType: string;
  userId: string;
  fromUserName: string | null;
  telegramMessageId: number | null;
  uploadSource: string;
  createdAt: string;
};

export type FileHistoryResponse = {
  status: number;
  msg: string;
  data: {
    content: FileHistoryItem[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  } | null;
};
