import Sidebar from "./components/Sidebar";
import AIChatWindow from "./components/AIChatWindow";
import ToDoElement from "./components/ToDoElement";
import { useState } from "react";

function App() {
	const [todoLists, setTodoLists] = useState([
		{ id: "1", name: "Finish report", date: "", completed: false },
		{ id: "2", name: "Buy groceries", date: "", completed: false },
	]);

	// Example logic for overdue (for demonstration)
	const today = new Date();
	const isOverdue = (date: string, completed: boolean) =>
		!completed && date && new Date(date).setHours(0, 0, 0, 0) < today.setHours(0, 0, 0, 0);

	const activeTasks = todoLists.filter(
		(todo) => !todo.completed && !isOverdue(todo.date, todo.completed)
	);
	const overdueTasks = todoLists.filter(
		(todo) => !todo.completed && isOverdue(todo.date, todo.completed)
	);
	const completedTasks = todoLists.filter((todo) => todo.completed);

	const handleDeleteCompleted = () => {
		setTodoLists(todoLists.filter((todo) => !todo.completed));
	};

	return (
		<>
			<Sidebar
				todoLists={todoLists}
				onAddTodoList={() => {
					throw new Error("Function not implemented.");
				}}
				onDeleteTodoList={() => {
					throw new Error("Function not implemented.");
				}}
			/>
			<div className='flex flex-col items-center justify-start min-h-screen bg-white dark:bg-gray-700 pt-8 pb-20'>
				{/* Active Section */}
				<section className='w-full flex flex-col items-center' style={{ height: "30vh" }}>
					<h2 className='text-2xl font-bold mb-2 text-black dark:text-white'>Active Tasks</h2>
					{activeTasks.length === 0 && <p className='text-gray-400'>No active tasks.</p>}
					{activeTasks.map((todo) => (
						<ToDoElement key={todo.id} initialText={todo.name} initialDate={todo.date} />
					))}
				</section>
				{/* Overdue Section */}
				<section className='w-full flex flex-col items-center' style={{ height: "30vh" }}>
					<h2 className='text-2xl font-bold mb-2 text-red-600 dark:text-red-400'>Overdue Tasks</h2>
					{overdueTasks.length === 0 && <p className='text-gray-400'>No overdue tasks.</p>}
					{overdueTasks.map((todo) => (
						<ToDoElement key={todo.id} initialText={todo.name} initialDate={todo.date} />
					))}
				</section>
				{/* Completed Section */}
				<section className='w-full flex flex-col items-center' style={{ height: "30vh" }}>
					<h2 className='text-2xl font-bold mb-2 text-green-600 dark:text-green-400'>
						Completed Tasks
					</h2>
					{completedTasks.length === 0 && <p className='text-gray-400'>No completed tasks.</p>}
					{completedTasks.map((todo) => (
						<ToDoElement key={todo.id} initialText={todo.name} initialDate={todo.date} />
					))}
				</section>
				{/* Delete Button */}
				<div className='w-full flex justify-center mt-6 absolute bottom-8 left-0'>
					<button
						className='bg-red-500 hover:bg-red-700 text-white font-semibold py-2 px-6 rounded shadow'
						onClick={handleDeleteCompleted}
					>
						Delete All Completed Tasks
					</button>
				</div>
			</div>
			<AIChatWindow />
		</>
	);
}

export default App;
