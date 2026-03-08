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
