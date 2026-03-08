export type WebhookResponse = {
  ok?: boolean;
  description?: string;
  result?: unknown;
};

export type SendMessageResponse = {
  status: string;
  message: string | null;
};

export type GetUpdatesResponse = {
  result: string;
};
