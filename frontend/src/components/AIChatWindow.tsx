import React, { useState, useRef, useEffect } from "react";
import { FiMessageSquare, FiX } from "react-icons/fi";
import axios from "axios";

interface AIChatWindowProps {
	onReload: () => void;
}

const AIChatWindow: React.FC<AIChatWindowProps> = ({ onReload }) => {
	const [input, setInput] = useState("");
	const [messages, setMessages] = useState<{ sender: "user" | "ai"; text: string }[]>([]);
	const [visible, setVisible] = useState(true);
	const messagesEndRef = useRef<HTMLDivElement | null>(null);

	useEffect(() => {
		messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
	}, [messages, visible]);

	const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
		setInput(e.target.value);
	};

	const handleSend = async () => {
		if (!input.trim()) return;
		const userMessage = { sender: "user" as const, text: input };
		setMessages((prev) => [...prev, userMessage]);
		setInput("");
		try {
			const res = await axios.post("http://localhost:8080/ai-chat", { prompt: input });
			setMessages((prev) => [...prev, { sender: "ai", text: res.data.response }]);
			if (res.data.reload) {
				onReload();
			}
		} catch (error) {
			setMessages((prev) => [...prev, { sender: "ai", text: "Error contacting AI backend." }]);
			console.error("AI chat error:", error);
		}
	};

	const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
		if (e.key === "Enter" && !e.shiftKey) {
			e.preventDefault();
			handleSend();
		}
	};

	return (
		<>
			{/* Toggle Button */}
			<button
				className='fixed top-4 right-4 z-30 bg-gray-900 dark:bg-white rounded-full p-2 shadow-md focus:outline-none'
				onClick={() => setVisible((v) => !v)}
				aria-label={visible ? "Hide AI chat" : "Show AI chat"}
			>
				{visible ? (
					<FiX size={24} className='text-white dark:text-black' />
				) : (
					<FiMessageSquare size={24} className='text-white dark:text-black' />
				)}
			</button>

			{/* Chat Window */}
			{visible && (
				<aside
					className='
                        fixed right-0 top-0 h-screen
                        w-[20vw] min-w-[240px] max-w-sm
                        bg-white dark:bg-gray-900
                        border-l border-gray-200 dark:border-gray-700
                        flex flex-col z-20
                        shadow-lg
                    '
				>
					{/* Messages */}
					<div className='flex-1 overflow-y-auto px-4 py-4 mt-14'>
						{messages.map((msg, idx) => (
							<div
								key={idx}
								className={`
                                mb-3 flex chat-message
                                ${msg.sender === "user" ? "justify-end" : "justify-start"}
                            `}
							>
								<span
									className={`
                                    inline-block px-4 py-2 rounded-2xl max-w-[70%] break-words
                                    ${
																			msg.sender === "user"
																				? "bg-green-100 dark:bg-green-800 text-gray-900 dark:text-gray-100"
																				: "bg-gray-200 dark:bg-gray-700 text-gray-900 dark:text-gray-100 chat-response"
																		}
                                `}
								>
									{msg.text}
								</span>
							</div>
						))}
						<div ref={messagesEndRef} />
					</div>

					{/* Input */}
					<div className='p-3 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900'>
						<form
							className='flex gap-2'
							onSubmit={(e) => {
								e.preventDefault();
								handleSend();
							}}
						>
							<textarea
								id='chat-input'
								value={input}
								onChange={handleInputChange}
								onKeyDown={handleKeyDown}
								rows={1}
								placeholder='Type your message...'
								className='
                                flex-1 resize-none rounded-2xl px-4 py-2
                                bg-gray-100 dark:bg-gray-800
                                border border-gray-300 dark:border-gray-700
                                text-gray-900 dark:text-gray-100
                                focus:outline-none focus:ring-2 focus:ring-blue-400
                                min-h-[40px] max-h-[120px]
                            '
							/>
							<button
								id='send-button'
								type='submit'
								disabled={!input.trim()}
								className={`
                                bg-blue-600 dark:bg-blue-500 hover:bg-blue-700 dark:hover:bg-blue-600
                                text-white rounded-full w-10 h-10 flex items-center justify-center
                                transition-colors disabled:opacity-50 disabled:cursor-not-allowed
                            `}
								aria-label='Send'
							>
								<svg width='20' height='20' viewBox='0 0 20 20' fill='none'>
									<path
										d='M4 10H16M16 10L11 5M16 10L11 15'
										stroke='currentColor'
										strokeWidth='2'
										strokeLinecap='round'
										strokeLinejoin='round'
									/>
								</svg>
							</button>
						</form>
					</div>
				</aside>
			)}
		</>
	);
};

export default AIChatWindow;
